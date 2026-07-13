package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ScheduleViewModel
import com.example.util.AutostartUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ScheduleViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val currentUrl by viewModel.appsScriptUrl.collectAsStateWithLifecycle()
    val currentSheetId by viewModel.spreadsheetId.collectAsStateWithLifecycle()
    val currentWeeklyUrl by viewModel.weeklyMeetingUrl.collectAsStateWithLifecycle()

    var urlInput by remember { mutableStateOf(currentUrl) }
    var sheetIdInput by remember { mutableStateOf(currentSheetId) }
    var weeklyUrlInput by remember { mutableStateOf(currentWeeklyUrl) }

    LaunchedEffect(currentUrl, currentSheetId, currentWeeklyUrl) {
        urlInput = currentUrl
        sheetIdInput = currentSheetId
        weeklyUrlInput = currentWeeklyUrl
    }

    val appsScriptCode = """
// SCRIPT DETEKSI & KONEKSI GOOGLE SHEETS UNTUK APLIKASI JADWAL FOTO, EDITING & WEEKLY MEETING (VERSI V6.0)
// Tempelkan kode ini di Google Apps Script spreadsheet JADWAL FOTO Anda (Ekstensi -> Apps Script)

function doGet(e) {
  try {
    var action = (e && e.parameter && e.parameter.action) || "";
    
    // 1. Aksi untuk mengambil data Weekly Meeting dari spreadsheet eksternal
    if (action === "get_weekly_meeting_listings") {
      return getWeeklyMeetingListings(e);
    }
    if (action === "get_all_weekly_meeting_listings") {
      return getAllWeeklyMeetingListings(e);
    }
    if (action === "get_yearly_ig_posting_history") {
      return getYearlyIgPostingHistory(e);
    }
    if (action === "get_absensi_meeting") {
      return getAbsensiMeeting(e);
    }
    
    // 2. Aksi Bawaan: Ambil Jadwal Foto & Edit Foto
    var sheetName = (e && e.parameter && e.parameter.sheetName) || "Juni 2026";
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName(sheetName) || ss.getSheets()[0];
    var maxRows = 304;
    var rawValues = sheet.getRange(1, 1, maxRows, 20).getValues(); // Ambil kolom A s/d T
    
    var schedules = [];
    var editFotoTasks = [];
    
    // Proses Jadwal Utama (Kolom A-J, baris 5 s/d 304)
    for (var i = 4; i < maxRows; i++) {
      var row = rawValues[i];
      if (!row) continue;
      
      var realNo = row[0] != null && row[0] !== "" ? parseInt(row[0]) : 0;
      var realIdListing = row[1] != null ? row[1].toString().trim() : "";
      var realNamaMe = row[2] != null ? row[2].toString().trim() : "";
      var realStaff = row[3] != null ? row[3].toString().trim() : "";
      var realTanggal = formatDateHelper(row[4]);
      var realLokasi = row[5] != null ? row[5].toString().trim() : "";
      var realJam = formatTimeHelper(row[6]);
      var rawType = row[7] != null ? row[7].toString().trim() : "";
      var rawStatus = row[8] != null ? row[8].toString().trim() : "";
      var realSource = row[9] != null ? row[9].toString().trim() : "";
      
      if (realIdListing === "" && realNamaMe === "" && realLokasi === "") {
        continue;
      }
      
      schedules.push({
        no: realNo || (i - 3),
        idListing: realIdListing,
        namaMe: realNamaMe,
        lokasi: realLokasi,
        staff: realStaff,
        tanggal: realTanggal,
        jam: realJam,
        type: rawType === "" ? "Foto" : rawType,
        status: rawStatus === "" ? "Pending" : rawStatus,
        source: realSource === "" ? "Spreadsheet" : realSource
      });
    }
    
    // Proses Edit Foto Posting IG (Kolom L-T, baris 5 s/d 304)
    for (var i = 4; i < maxRows; i++) {
      var row = rawValues[i];
      if (!row) continue;
      
      var editNo = row[11] != null && row[11] !== "" ? parseInt(row[11]) : 0;
      var editIdListing = row[12] != null ? row[12].toString().trim() : "";
      var editNamaMe = row[13] != null ? row[13].toString().trim() : "";
      var rawPostingIg = row[14] != null ? row[14].toString().trim() : "";
      
      var editJadwal = "";
      if (row[15] != null && row[15] !== "") {
        if (row[15] instanceof Date) {
          editJadwal = formatDateDMYHelper(row[15]);
        } else {
          editJadwal = row[15].toString().trim();
          if (editJadwal.indexOf("GMT") !== -1 || editJadwal.indexOf("00:00:00") !== -1) {
            try {
              var d = new Date(editJadwal);
              if (!isNaN(d.getTime())) {
                editJadwal = formatDateDMYHelper(d);
              }
            } catch (e) {}
          }
        }
      }

      var editNotesVal = row[16] != null ? row[16].toString().trim() : "";
      var rawDone = row[17] != null ? row[17].toString().trim() : "";
      var editJudul = row[18] != null ? row[18].toString().trim() : "";
      var editSource = row[19] != null ? row[19].toString().trim() : "";
      
      if (editIdListing === "" && editNamaMe === "" && editNotesVal === "") {
        continue;
      }
      
      var isPostingIg = (rawPostingIg.toLowerCase() === "done" || rawPostingIg.toLowerCase() === "ya" || rawPostingIg.toLowerCase() === "yes" || rawPostingIg.toLowerCase() === "true" || rawPostingIg === "v" || rawPostingIg === "✔" || rawPostingIg === "1");
      var isDone = (rawDone.toLowerCase() === "done" || rawDone.toLowerCase() === "ya" || rawDone.toLowerCase() === "yes" || rawDone.toLowerCase() === "true" || rawDone === "v" || rawDone === "✔" || rawDone === "1");
      
      editFotoTasks.push({
        no: editNo || (i - 3),
        idListing: editIdListing,
        namaMe: editNamaMe,
        postingIg: isPostingIg,
        jadwalPosting: editJadwal,
        editNotes: editNotesVal,
        done: isDone,
        judul: editJudul,
        source: editSource
      });
    }
    
    var responseObj = {
      schedules: schedules,
      editFotoTasks: editFotoTasks
    };
    
    return ContentService.createTextOutput(JSON.stringify(responseObj))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ status: "error", message: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function formatDateHelper(val) {
  if (val == null) return "";
  if (val instanceof Date) {
    var year = val.getFullYear();
    var month = ("0" + (val.getMonth() + 1)).slice(-2);
    var date = ("0" + val.getDate()).slice(-2);
    if (year === 1899) return ""; 
    return year + "-" + month + "-" + date;
  }
  var str = val.toString().trim();
  if (str === "") return "";
  if (str.match(/^\d{4}-\d{2}-\d{2}/)) {
    return str.substring(0, 10);
  }
  
  var match = str.match(/^(\d{1,2})[\/\-](\d{1,2})[\/\-](\d{2,4})/);
  if (match) {
    var d = parseInt(match[1], 10);
    var m = parseInt(match[2], 10);
    var y = parseInt(match[3], 10);
    if (y < 100) {
      y += (y < 50 ? 2000 : 1900);
    }
    var mm = ("0" + m).slice(-2);
    var dd = ("0" + d).slice(-2);
    return y + "-" + mm + "-" + dd;
  }
  return str;
}

function formatDateDMYHelper(val) {
  if (val == null) return "";
  if (val instanceof Date) {
    var year = val.getFullYear();
    var month = ("0" + (val.getMonth() + 1)).slice(-2);
    var date = ("0" + val.getDate()).slice(-2);
    if (year === 1899) return ""; 
    return date + "/" + month + "/" + year;
  }
  return val.toString().trim();
}

function formatTimeHelper(val) {
  if (val == null) return "";
  if (val instanceof Date) {
    var hours = ("0" + val.getHours()).slice(-2);
    var minutes = ("0" + val.getMinutes()).slice(-2);
    return hours + ":" + minutes;
  }
  var str = val.toString().trim();
  var match = str.match(/(\d{2}):(\d{2})/);
  if (match) {
    return match[1] + ":" + match[2];
  }
  return str;
}

function doPost(e) {
  try {
    var params = {};
    if (e && e.postData && e.postData.contents) {
      params = JSON.parse(e.postData.contents);
    } else if (e && e.parameter) {
      params = e.parameter;
    }
    
    var action = params.action || "add";
    
    // Aksi baru untuk menambahkan data Weekly Meeting ke spreadsheet eksternal
    if (action === "add_weekly_meeting_listing") {
      return addWeeklyMeetingListing(params);
    }
    if (action === "update_weekly_meeting_posting_ig") {
      return updateWeeklyMeetingPostingIg(params);
    }
    if (action === "update_weekly_meeting_details") {
      return updateWeeklyMeetingDetails(params);
    }
    if (action === "update_weekly_meeting_schedule") {
      return updateWeeklyMeetingSchedule(params);
    }
    if (action === "update_absensi_meeting") {
      return updateAbsensiMeeting(params);
    }
    
    var sheetName = params.sheetName || (e && e.parameter && e.parameter.sheetName) || "Juni 2026";
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName(sheetName) || ss.getSheets()[0];
    var maxRows = 304;
    
    var lookupRows = sheet.getRange(1, 1, maxRows, 20).getValues();
    
    // ACTION EDIT FOTO TASK (KOLOM L-T)
    if (action === "edit_foto") {
      var editNoTarget = params.no || 0;
      var editIdListingTarget = params.idListing || "";
      
      var foundRowIndex = -1;
      for (var i = 4; i < maxRows; i++) {
        var row = lookupRows[i];
        if (!row) continue;
        var rowEditNo = row[11] != null && row[11] !== "" ? parseInt(row[11]) : 0;
        var rowIdListing = row[12] != null ? row[12].toString().trim() : "";
        
        if (editNoTarget > 0 && rowEditNo === editNoTarget) {
          foundRowIndex = i;
          break;
        } else if (editIdListingTarget !== "" && rowIdListing === editIdListingTarget) {
          foundRowIndex = i;
          break;
        }
      }
      
      if (foundRowIndex !== -1) {
        var targetRow = foundRowIndex + 1;
        var postingIgVal = params.postingIg ? true : false;
        var doneVal = params.done ? true : false;
        
        var editJadwalVal = params.jadwalPosting || "";
        if (editJadwalVal !== "") {
          if (editJadwalVal.indexOf("GMT") !== -1 || editJadwalVal.indexOf("00:00:00") !== -1) {
            try {
              var d = new Date(editJadwalVal);
              if (!isNaN(d.getTime())) {
                editJadwalVal = formatDateDMYHelper(d);
              }
            } catch(e) {}
          } else {
            var matchYMD = editJadwalVal.match(/^(\d{4})-(\d{2})-(\d{2})/);
            if (matchYMD) {
              editJadwalVal = matchYMD[3] + "/" + matchYMD[2] + "/" + matchYMD[1];
            }
          }
        }
        
        sheet.getRange(targetRow, 13).setValue(params.idListing || ""); // Kolom M
        sheet.getRange(targetRow, 14).setValue(params.namaMe || ""); // Kolom N
        sheet.getRange(targetRow, 15).setValue(postingIgVal); // Kolom O
        sheet.getRange(targetRow, 16).setValue(editJadwalVal); // Kolom P
        sheet.getRange(targetRow, 17).setValue(params.editNotes || ""); // Kolom Q
        sheet.getRange(targetRow, 18).setValue(doneVal); // Kolom R
        sheet.getRange(targetRow, 19).setValue(params.judul || ""); // Kolom S
        sheet.getRange(targetRow, 20).setValue(params.source || ""); // Kolom T
        
        try {
          var idListingVal = params.idListing || "";
          if (idListingVal.toString().trim() !== "") {
            var eMock = { range: sheet.getRange(targetRow, 13), value: idListingVal };
            onEditAutoFillSheet3(eMock);
          }
        } catch(eErr) {}
        try {
          sortEditFoto(sheet);
        } catch(eErr) {}
        
        return ContentService.createTextOutput(JSON.stringify({
          status: "success",
          message: "Edit Foto Task berhasil diperbarui di baris " + targetRow
        })).setMimeType(ContentService.MimeType.JSON);
      } else {
        return ContentService.createTextOutput(JSON.stringify({
          status: "error",
          message: "Gagal menemukan baris Task Edit Foto yang cocok."
        })).setMimeType(ContentService.MimeType.JSON);
      }
    }
    
    // ACTION DELETE EDIT FOTO TASK
    if (action === "delete_foto") {
      var editNoTarget = params.no || 0;
      var editIdListingTarget = params.idListing || "";
      
      var foundRowIndex = -1;
      for (var i = 4; i < maxRows; i++) {
        var row = lookupRows[i];
        if (!row) continue;
        var rowEditNo = row[11] != null && row[11] !== "" ? parseInt(row[11]) : 0;
        var rowIdListing = row[12] != null ? row[12].toString().trim() : "";
        
        if (editNoTarget > 0 && rowEditNo === editNoTarget) {
          foundRowIndex = i;
          break;
        } else if (editIdListingTarget !== "" && rowIdListing === editIdListingTarget) {
          foundRowIndex = i;
          break;
        }
      }
      
      if (foundRowIndex !== -1) {
        var targetRow = foundRowIndex + 1;
        sheet.getRange(targetRow, 13).setValue(""); // Kolom M
        sheet.getRange(targetRow, 14).setValue(""); // Kolom N
        sheet.getRange(targetRow, 15).setValue(""); // Kolom O
        sheet.getRange(targetRow, 16).setValue(""); // Kolom P
        sheet.getRange(targetRow, 17).setValue(""); // Kolom Q
        sheet.getRange(targetRow, 18).setValue(""); // Kolom R
        sheet.getRange(targetRow, 19).setValue(""); // Kolom S
        sheet.getRange(targetRow, 20).setValue(""); // Kolom T
        
        try {
          sortEditFoto(sheet);
        } catch(eErr) {}
        
        return ContentService.createTextOutput(JSON.stringify({
          status: "success",
          message: "Task Edit Foto berhasil dihapus di baris " + targetRow
        })).setMimeType(ContentService.MimeType.JSON);
      } else {
        return ContentService.createTextOutput(JSON.stringify({
          status: "error",
          message: "Gagal menemukan baris Task Edit Foto yang ingin dihapus."
        })).setMimeType(ContentService.MimeType.JSON);
      }
    }
    
    // ACTION DELETE JADWAL UTAMA
    if (action === "delete") {
      var idListingTarget = params.idListing || "";
      var namaMeTarget = params.namaMe || "";
      var tanggalTarget = formatDateHelper(params.tanggal) || "";
      var jamTarget = formatTimeHelper(params.jam) || "";
      var typeTarget = params.type || "";
      
      var foundRowIndex = -1;
      for (var i = 4; i < maxRows; i++) {
        var row = lookupRows[i];
        if (!row) continue;
        var rowIdListing = row[1] != null ? row[1].toString().trim() : "";
        var rowNamaMe = row[2] != null ? row[2].toString().trim() : "";
        var rowTanggal = formatDateHelper(row[4]);
        var rowJam = formatTimeHelper(row[6]);
        var rowType = row[7] != null ? row[7].toString().trim() : "";
        
        var matches = false;
        if (typeTarget !== "" && rowType === typeTarget) {
          matches = true;
        } else if (idListingTarget !== "" && rowIdListing === idListingTarget) {
          matches = true;
        } else if (idListingTarget === "" && rowNamaMe === namaMeTarget && rowTanggal === tanggalTarget && rowJam === jamTarget) {
          matches = true;
        }
        
        if (matches) {
          foundRowIndex = i;
          break;
        }
      }
      
      if (foundRowIndex !== -1) {
        var rowToClear = foundRowIndex + 1;
        sheet.getRange(rowToClear, 2).setValue(""); // ID Listing (B)
        sheet.getRange(rowToClear, 3).setValue(""); // Nama ME (C)
        sheet.getRange(rowToClear, 4).setValue(""); // Staff (D)
        sheet.getRange(rowToClear, 5).setValue(""); // Fix Date (E)
        sheet.getRange(rowToClear, 6).setValue(""); // Lokasi (F)
        sheet.getRange(rowToClear, 7).setValue(""); // Jam (G)
        sheet.getRange(rowToClear, 8).setValue(""); // Type (H)
        sheet.getRange(rowToClear, 9).setValue(""); // Status (I)
        sheet.getRange(rowToClear, 10).setValue(""); // Source (J)
        
        return ContentService.createTextOutput(JSON.stringify({
          status: "success",
          message: "Data berhasil dihapus dari baris " + rowToClear
        })).setMimeType(ContentService.MimeType.JSON);
      } else {
        return ContentService.createTextOutput(JSON.stringify({
          status: "error",
          message: "Gagal menemukan baris data yang cocok untuk dihapus."
        })).setMimeType(ContentService.MimeType.JSON);
      }
    }
    
    // ACTION EDIT JADWAL UTAMA
    if (action === "edit" || action === "update") {
      var noTarget = params.no || 0;
      var idListingTarget = params.originalIdListing || params.idListing || "";
      var namaMeTarget = params.originalNamaMe || params.namaMe || "";
      var tanggalTarget = formatDateHelper(params.originalTanggal || params.tanggal) || "";
      var jamTarget = formatTimeHelper(params.originalJam || params.jam) || "";
      
      var foundRowIndex = -1;
      for (var i = 4; i < maxRows; i++) {
        var row = lookupRows[i];
        if (!row) continue;
        
        var rowNo = row[0] != null && row[0] !== "" ? parseInt(row[0]) : 0;
        var rowIdListing = row[1] != null ? row[1].toString().trim() : "";
        var rowNamaMe = row[2] != null ? row[2].toString().trim() : "";
        var rowTanggal = formatDateHelper(row[4]);
        var rowJam = formatTimeHelper(row[6]);
        
        var matches = false;
        if (noTarget > 0 && rowNo === noTarget) {
          matches = true;
        } else if (noTarget === 0) {
          if (idListingTarget !== "" && rowIdListing === idListingTarget) {
            matches = true;
          } else if (idListingTarget === "" && rowNamaMe === namaMeTarget && rowTanggal === tanggalTarget && rowJam === jamTarget) {
            matches = true;
          }
        }
        
        if (matches) {
          foundRowIndex = i;
          break;
        }
      }
      
      if (foundRowIndex !== -1) {
        var targetRow = foundRowIndex + 1;
        
        sheet.getRange(targetRow, 2).setValue(params.idListing || "");
        sheet.getRange(targetRow, 3).setValue(params.namaMe || "");
        sheet.getRange(targetRow, 4).setValue(params.staff || "");
        sheet.getRange(targetRow, 5).setValue(formatDateHelper(params.tanggal) || "");
        sheet.getRange(targetRow, 6).setValue(params.lokasi || "");
        sheet.getRange(targetRow, 7).setValue(formatTimeHelper(params.jam) || "");
        sheet.getRange(targetRow, 8).setValue(params.type || "Foto");
        
        var statusCell = sheet.getRange(targetRow, 9);
        var statusVal = params.status || "Pending";
        statusCell.setValue(statusVal);
        if (statusVal.toString().toUpperCase() === "DONE") {
          statusCell.setBackground("#00FF00");
          statusCell.setFontColor("#000000");
          statusCell.setHorizontalAlignment("center");
        } else {
          statusCell.setBackground(null);
          statusCell.setFontColor(null);
          statusCell.setHorizontalAlignment("left");
        }
        
        sheet.getRange(targetRow, 10).setValue(params.source || "App");
        
        try {
          var idListingVal = params.idListing || "";
          if (idListingVal.toString().trim() !== "") {
            var eMock = { range: sheet.getRange(targetRow, 2), value: idListingVal };
            onEditAutoFillSheet3(eMock);
          }
        } catch(eErr) {}
        try {
          sortFotoUlang(sheet);
        } catch(eErr) {}
        
        return ContentService.createTextOutput(JSON.stringify({ 
          status: "success", 
          message: "Data berhasil diperbarui di baris " + targetRow,
          row: targetRow
        })).setMimeType(ContentService.MimeType.JSON);
      } else {
        return ContentService.createTextOutput(JSON.stringify({ 
          status: "error", 
          message: "Gagal menemukan baris jadwal yang ingin diedit." 
        })).setMimeType(ContentService.MimeType.JSON);
      }
    }
    
    // ACTION ADD JADWAL UTAMA
    var idListing = params.idListing || "";
    var namaMe = params.namaMe || "";
    var lokasi = params.lokasi || "";
    var tanggal = formatDateHelper(params.tanggal) || "";
    var jam = formatTimeHelper(params.jam) || "";
    var staff = params.staff || "";
    var type = params.type || "Foto";
    var status = params.status || "Pending";
    var source = params.source || "App";
    
    var targetRow = -1;
    for (var i = 4; i < maxRows; i++) {
      var row = lookupRows[i];
      if (!row) continue;
      var checkListing = row[1] != null ? row[1].toString().trim() : "";
      var checkNamaMe = row[2] != null ? row[2].toString().trim() : "";
      var checkLokasi = row[5] != null ? row[5].toString().trim() : "";
      
      if (checkListing === "" && checkNamaMe === "" && checkLokasi === "") {
        targetRow = i + 1;
        break;
      }
    }
    
    if (targetRow === -1) {
      targetRow = sheet.getLastRow() + 1;
    }
    
    sheet.getRange(targetRow, 2).setValue(idListing);
    sheet.getRange(targetRow, 3).setValue(namaMe);
    sheet.getRange(targetRow, 4).setValue(staff);
    sheet.getRange(targetRow, 5).setValue(tanggal);
    sheet.getRange(targetRow, 6).setValue(lokasi);
    sheet.getRange(targetRow, 7).setValue(jam);
    sheet.getRange(targetRow, 8).setValue(type);
    
    var statusCell = sheet.getRange(targetRow, 9);
    statusCell.setValue(status);
    if (status.toString().toUpperCase() === "DONE") {
      statusCell.setBackground("#00FF00");
      statusCell.setFontColor("#000000");
      statusCell.setHorizontalAlignment("center");
    } else {
      statusCell.setBackground(null);
      statusCell.setFontColor(null);
      statusCell.setHorizontalAlignment("left");
    }
    
    sheet.getRange(targetRow, 10).setValue(source);
    
    try {
      if (idListing && idListing.toString().trim() !== "") {
        var eMock = { range: sheet.getRange(targetRow, 2), value: idListing };
        onEditAutoFillSheet3(eMock);
      }
    } catch(eErr) {}
    try {
      sortFotoUlang(sheet);
    } catch(eErr) {}
    
    return ContentService.createTextOutput(JSON.stringify({ 
      status: "success", 
      message: "Data berhasil disimpan di baris " + targetRow,
      row: targetRow
    })).setMimeType(ContentService.MimeType.JSON);
  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({ status: "error", message: error.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

// ==========================================
// FUNGSI HELPER EKSTERNAL WEEKLY MEETING
// ==========================================
function findSheetByFlexibleName(ss, sheetName) {
  var sheet = ss.getSheetByName(sheetName);
  if (sheet) return sheet;
  
  // Bersihkan sheetName (hapus prefiks "Recap Meeting " dan tahun jika ada)
  var clean = sheetName.replace("Recap Meeting ", "").replace(/\s*\d{4}/g, "").trim();
  
  // Ambil tahun dari input atau default ke tahun saat ini (default 2026 untuk spreadsheet)
  var yearMatch = sheetName.match(/\b(\d{4})\b/);
  var year = yearMatch ? yearMatch[1] : "2026";
  
  // Coba berbagai kombinasi nama sheet
  var options = [
    clean + " " + year,
    "Recap Meeting " + clean,
    clean
  ];
  
  for (var i = 0; i < options.length; i++) {
    sheet = ss.getSheetByName(options[i]);
    if (sheet) return sheet;
  }
  return null;
}

function getWeeklyMeetingListings(e) {
  var sheetName = e.parameter.sheetName; // e.g. "Recap Meeting Juni" atau "Recap Meeting Juli"
  var dateStr = e.parameter.date; // Format YYYY-MM-DD
  
  var weeklyMeetingSpreadsheetId = "1ydmss-ADSeJpw7KJyQzT44RUNaqu5wJ0UJrIxn_8EmY";
  var ss = SpreadsheetApp.openById(weeklyMeetingSpreadsheetId);
  var sheet = findSheetByFlexibleName(ss, sheetName);
  
  if (!sheet) {
    var sheets = ss.getSheets();
    var names = sheets.map(function(s) { return s.getName(); });
    return ContentService.createTextOutput(JSON.stringify({
      "status": "error",
      "message": "Sheet dengan nama '" + sheetName + "' tidak ditemukan. Tersedia sheet: " + names.join(", ")
    })).setMimeType(ContentService.MimeType.JSON);
  }
  
  var colInfo = getMeetingColumnAndMaxRow(dateStr, sheet);
  if (!colInfo) {
    return ContentService.createTextOutput(JSON.stringify({
      "status": "error",
      "message": "Pemetaan kolom tidak ditemukan untuk tanggal: " + dateStr
    })).setMimeType(ContentService.MimeType.JSON);
  }
  
  var startRow = 5;
  var colIndex = colInfo.col;
  var maxRow = colInfo.maxRow;
  var numRows = maxRow - startRow + 1;
  
  var dataRange = sheet.getRange(startRow, colIndex, numRows, 6);
  var values = dataRange.getValues();
  
  var listings = [];
  for (var r = 0; r < values.length; r++) {
    var row = values[r];
    var idListing = row[0] ? row[0].toString().trim() : "";
    if (idListing !== "") {
      listings.push({
        "no": r + 1,
        "idListing": idListing,
        "keterangan": row[1] ? row[1].toString().trim() : "",
        "postingIg": row[2] ? row[2].toString().trim() : "",
        "jadwalPosting": row[3] ? row[3].toString().trim() : "",
        "namaMe": row[4] ? row[4].toString().trim() : "",
        "catatan": row[5] ? row[5].toString().trim() : ""
      });
    }
  }
  
  return ContentService.createTextOutput(JSON.stringify({
    "status": "success",
    "sheetName": sheet.getName(),
    "date": dateStr,
    "listings": listings
  })).setMimeType(ContentService.MimeType.JSON);
}

function addWeeklyMeetingListing(data) {
  var sheetName = data.sheetName; // e.g. "Recap Meeting Juni" atau "Recap Meeting Juli"
  var dateStr = data.date; // e.g. "2026-06-02"
  var idListing = data.idListing.toString().trim();
  var namaMe = data.namaMe ? data.namaMe.toString().trim() : "";
  var keterangan = data.keterangan ? data.keterangan.toString().trim() : "";
  var catatan = data.catatan ? data.catatan.toString().trim() : "";
  
  var weeklyMeetingSpreadsheetId = "1ydmss-ADSeJpw7KJyQzT44RUNaqu5wJ0UJrIxn_8EmY";
  var ss = SpreadsheetApp.openById(weeklyMeetingSpreadsheetId);
  var sheet = findSheetByFlexibleName(ss, sheetName);
  
  if (!sheet) {
    return ContentService.createTextOutput(JSON.stringify({
      "status": "error",
      "message": "Sheet dengan nama '" + sheetName + "' tidak ditemukan."
    })).setMimeType(ContentService.MimeType.JSON);
  }
  
  var colInfo = getMeetingColumnAndMaxRow(dateStr, sheet);
  if (!colInfo) {
    return ContentService.createTextOutput(JSON.stringify({
      "status": "error",
      "message": "Pemetaan kolom tidak ditemukan untuk tanggal: " + dateStr
    })).setMimeType(ContentService.MimeType.JSON);
  }
  
  var startRow = 5;
  var colIndex = colInfo.col;
  var maxRow = colInfo.maxRow;
  
  var targetRow = -1;
  for (var r = startRow; r <= maxRow; r++) {
    var cellValue = sheet.getRange(r, colIndex).getValue().toString().trim();
    if (cellValue === "") {
      targetRow = r;
      break;
    }
  }
  
  if (targetRow === -1) {
    return ContentService.createTextOutput(JSON.stringify({
      "status": "error",
      "message": "Kolom untuk tanggal " + dateStr + " sudah penuh (maksimal baris " + maxRow + ")."
    })).setMimeType(ContentService.MimeType.JSON);
  }
  
  sheet.getRange(targetRow, colIndex).setValue(idListing);      // Kolom 1: ID Listing
  sheet.getRange(targetRow, colIndex + 1).setValue(keterangan); // Kolom 2: Keterangan
  var igCell = sheet.getRange(targetRow, colIndex + 2);         // Kolom 3: Posting IG
  igCell.setDataValidation(SpreadsheetApp.newDataValidation().requireCheckbox().build());
  igCell.setValue(false);
  sheet.getRange(targetRow, colIndex + 3).setValue("");         // Kolom 4: Jadwal Posting
  sheet.getRange(targetRow, colIndex + 4).setValue(namaMe);     // Kolom 5: Nama ME
  sheet.getRange(targetRow, colIndex + 5).setValue(catatan);    // Kolom 6: Catatan
  
  return ContentService.createTextOutput(JSON.stringify({
    "status": "success",
    "message": "Berhasil menambahkan listing " + idListing + " ke tanggal " + dateStr
  })).setMimeType(ContentService.MimeType.JSON);
}

function getMeetingColumnAndMaxRow(dateStr, sheet) {
  var parts = dateStr.split("-");
  var year = parseInt(parts[0], 10);
  var month = parseInt(parts[1], 10);
  var day = parseInt(parts[2], 10);
  
  // 1. Coba pencarian dinamis di baris header (baris 1 s/d 4)
  if (sheet) {
    var lastCol = sheet.getLastColumn();
    var checkCols = [2, 10, 18, 26, 34, 42, 50, 58];
    for (var i = 0; i < checkCols.length; i++) {
      var col = checkCols[i];
      if (col > lastCol) break;
      
      for (var r = 1; r <= 4; r++) {
        var val = sheet.getRange(r, col).getValue();
        if (!val) continue;
        
        if (val instanceof Date) {
          if (val.getDate() === day && (val.getMonth() + 1) === month) {
            return { col: col, maxRow: 120 };
          }
        }
        
        var valStr = val.toString().toLowerCase();
        var numMatch = valStr.match(/\b(\d{1,2})\b/);
        var cellDay = numMatch ? parseInt(numMatch[1], 10) : null;
        if (cellDay === day) {
          var indonesianMonths = ["januari", "februari", "maret", "april", "mei", "juni", "juli", "agustus", "september", "oktober", "november", "desember"];
          var englishMonths = ["january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"];
          var monthNameIndo = indonesianMonths[month - 1];
          var monthNameEng = englishMonths[month - 1];
          
          if (valStr.indexOf(monthNameIndo) !== -1 || valStr.indexOf(monthNameEng) !== -1 || valStr.indexOf("/" + month + "/") !== -1 || valStr.indexOf("-" + ("0" + month).slice(-2) + "-") !== -1) {
            return { col: col, maxRow: 120 };
          }
        }
      }
    }
  }
  
  // 2. Pemetaan statis untuk tahun 2026 berdasarkan data user
  if (year === 2026) {
    var mappings = {
      1: [6, 13, 20, 27],
      2: [3, 10, 17, 24],
      3: [3, 10, 17, 24, 31],
      4: [7, 14, 21, 28],
      5: [5, 12, 19, 26],
      6: [2, 9, 16, 23, 30],
      7: [7, 14, 21, 28],
      8: [4, 11, 18, 25],
      9: [1, 8, 15, 22, 29],
      10: [6, 13, 20, 27],
      11: [3, 10, 17, 24],
      12: [1, 8, 15, 22, 29]
    };
    
    var monthDays = mappings[month];
    if (monthDays) {
      for (var t = 0; t < monthDays.length; t++) {
        if (Math.abs(monthDays[t] - day) <= 1) {
          var colIndex = 2 + (t * 8);
          return { col: colIndex, maxRow: 120 };
        }
      }
    }
  }
  
  // 3. Fallback matematika hari Selasa jika di tahun lain
  var tuesdays = [];
  var d = new Date(year, month - 1, 1);
  while (d.getDay() !== 2) {
    d.setDate(d.getDate() + 1);
  }
  while (d.getMonth() === month - 1) {
    tuesdays.push(d.getDate());
    d.setDate(d.getDate() + 7);
  }
  
  for (var t = 0; t < tuesdays.length; t++) {
    if (Math.abs(tuesdays[t] - day) <= 1) { // toleransi 1 hari (misal Senin ke Selasa)
      var calculatedCol = 2 + (t * 8);
      return { col: calculatedCol, maxRow: 120 };
    }
  }
  
  return null;
}

function getAllWeeklyMeetingListings(e) {
  var sheetName = e.parameter.sheetName;
  var weeklyMeetingSpreadsheetId = "1ydmss-ADSeJpw7KJyQzT44RUNaqu5wJ0UJrIxn_8EmY";
  var ss = SpreadsheetApp.openById(weeklyMeetingSpreadsheetId);
  var sheet = findSheetByFlexibleName(ss, sheetName);
  
  if (!sheet) {
    var sheets = ss.getSheets();
    var names = sheets.map(function(s) { return s.getName(); });
    return ContentService.createTextOutput(JSON.stringify({
      "status": "error",
      "message": "Sheet '" + sheetName + "' not found. Available: " + names.join(", ")
    })).setMimeType(ContentService.MimeType.JSON);
  }
  
  var sName = sheet.getName();
  var cleanMonth = sName.replace("Recap Meeting ", "").replace(/\s*\d{4}/g, "").trim();
  var indonesianMonths = ["Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember"];
  var monthIndex = indonesianMonths.indexOf(cleanMonth);
  if (monthIndex === -1) {
    var lowerMonths = indonesianMonths.map(function(m) { return m.toLowerCase(); });
    monthIndex = lowerMonths.indexOf(cleanMonth.toLowerCase());
  }
  if (monthIndex === -1) monthIndex = 5; // fallback ke Juni
  
  var yearMatch = sName.match(/\b(\d{4})\b/);
  var sheetYear = yearMatch ? parseInt(yearMatch[1], 10) : 2026;
  
  var monthNum = monthIndex + 1;
  var monthStr = ("0" + monthNum).slice(-2);
  
  var dateCols = [];
  var checkCols = [2, 10, 18, 26, 34, 42, 50, 58];
  for (var i = 0; i < checkCols.length; i++) {
    var col = checkCols[i];
    if (col > sheet.getLastColumn()) break;
    
    var foundDay = null;
    for (var r = 1; r <= 4; r++) {
      var val = sheet.getRange(r, col).getValue();
      if (!val) continue;
      if (val instanceof Date) {
        foundDay = val.getDate();
        break;
      }
      var valStr = val.toString().toLowerCase();
      var numMatch = valStr.match(/\b(\d{1,2})\b/);
      if (numMatch) {
        foundDay = parseInt(numMatch[1], 10);
        break;
      }
    }
    
    if (foundDay !== null) {
      var dayStr = ("0" + foundDay).slice(-2);
      dateCols.push({ col: col, date: sheetYear + "-" + monthStr + "-" + dayStr, maxRow: 120 });
    } else {
      var tuesdays = [];
      var d = new Date(sheetYear, monthIndex, 1);
      while (d.getDay() !== 2) {
        d.setDate(d.getDate() + 1);
      }
      while (d.getMonth() === monthIndex) {
        tuesdays.push(d.getDate());
        d.setDate(d.getDate() + 7);
      }
      if (i < tuesdays.length) {
        var dayStr = ("0" + tuesdays[i]).slice(-2);
        dateCols.push({ col: col, date: sheetYear + "-" + monthStr + "-" + dayStr, maxRow: 120 });
      }
    }
  }
  
  var listings = [];
  for (var d = 0; d < dateCols.length; d++) {
    var colInfo = dateCols[d];
    var colIndex = colInfo.col;
    var dateStr = colInfo.date;
    var maxRow = colInfo.maxRow;
    var startRow = 5;
    var numRows = maxRow - startRow + 1;
    
    var dataRange = sheet.getRange(startRow, colIndex, numRows, 6);
    var values = dataRange.getValues();
    
    for (var r = 0; r < values.length; r++) {
      var row = values[r];
      var idListing = row[0] ? row[0].toString().trim() : "";
      var keterangan = row[1] ? row[1].toString().trim() : "";
      if (idListing !== "") {
        listings.push({
          "no": (startRow + r),
          "date": dateStr,
          "colIndex": colIndex,
          "idListing": idListing,
          "keterangan": keterangan,
          "postingIg": row[2] ? row[2].toString().trim() : "",
          "jadwalPosting": row[3] ? row[3].toString().trim() : "",
          "namaMe": row[4] ? row[4].toString().trim() : "",
          "catatan": row[5] ? row[5].toString().trim() : ""
        });
      }
    }
  }
  
  return ContentService.createTextOutput(JSON.stringify({
    "status": "success",
    "sheetName": sheet.getName(),
    "listings": listings
  })).setMimeType(ContentService.MimeType.JSON);
}

function updateWeeklyMeetingPostingIg(data) {
  var sheetName = data.sheetName;
  var dateStr = data.date;
  var row = parseInt(data.row);
  var col = parseInt(data.colIndex);
  var postingIgVal = data.postingIg ? true : false;
  
  var weeklyMeetingSpreadsheetId = "1ydmss-ADSeJpw7KJyQzT44RUNaqu5wJ0UJrIxn_8EmY";
  var ss = SpreadsheetApp.openById(weeklyMeetingSpreadsheetId);
  var sheet = findSheetByFlexibleName(ss, sheetName);
  
  if (!sheet) {
    return ContentService.createTextOutput(JSON.stringify({
      "status": "error",
      "message": "Sheet not found"
    })).setMimeType(ContentService.MimeType.JSON);
  }
  
  var targetCell = sheet.getRange(row, col + 2);
  targetCell.clearContent();
  targetCell.setDataValidation(SpreadsheetApp.newDataValidation().requireCheckbox().build());
  targetCell.setValue(postingIgVal === true);
  SpreadsheetApp.flush();
  
  var idCell = sheet.getRange(row, col);
  var jadwalCell = sheet.getRange(row, col + 3);
  
  if (data.postingIg) {
    idCell.setBackground("#00FF00");
    idCell.setFontColor("#000000");
    jadwalCell.setBackground("#00FF00");
    jadwalCell.setFontColor("#000000");
  } else {
    idCell.setBackground(null);
    idCell.setFontColor(null);
    jadwalCell.setBackground(null);
    jadwalCell.setFontColor(null);
  }
  
  return ContentService.createTextOutput(JSON.stringify({
    "status": "success",
    "message": "Berhasil memperbarui status Posting IG di sheet " + sheet.getName() + " baris " + row
  })).setMimeType(ContentService.MimeType.JSON);
}

function updateWeeklyMeetingDetails(data) {
  var sheetName = data.sheetName;
  var dateStr = data.date;
  var row = parseInt(data.row);
  var col = parseInt(data.colIndex);
  var idListing = data.idListing.toString().trim();
  var keterangan = data.keterangan ? data.keterangan.toString().trim() : "";
  var namaMe = data.namaMe ? data.namaMe.toString().trim() : "";
  var catatan = data.catatan ? data.catatan.toString().trim() : "";
  
  var weeklyMeetingSpreadsheetId = "1ydmss-ADSeJpw7KJyQzT44RUNaqu5wJ0UJrIxn_8EmY";
  var ss = SpreadsheetApp.openById(weeklyMeetingSpreadsheetId);
  var sheet = findSheetByFlexibleName(ss, sheetName);
  
  if (!sheet) {
    return ContentService.createTextOutput(JSON.stringify({
      "status": "error",
      "message": "Sheet not found"
    })).setMimeType(ContentService.MimeType.JSON);
  }
  
  sheet.getRange(row, col).setValue(idListing);      // Kolom 1: ID Listing
  sheet.getRange(row, col + 1).setValue(keterangan); // Kolom 2: Keterangan
  sheet.getRange(row, col + 4).setValue(namaMe);     // Kolom 5: Nama ME
  sheet.getRange(row, col + 5).setValue(catatan);    // Kolom 6: Catatan
  
  SpreadsheetApp.flush();
  
  return ContentService.createTextOutput(JSON.stringify({
    "status": "success",
    "message": "Berhasil memperbarui data listing " + idListing
  })).setMimeType(ContentService.MimeType.JSON);
}

function updateWeeklyMeetingSchedule(data) {
  var sheetName = data.sheetName;
  var row = parseInt(data.row);
  var col = parseInt(data.colIndex);
  var jadwalPosting = data.jadwalPosting ? data.jadwalPosting.toString().trim() : "";
  
  if (!sheetName || isNaN(row) || isNaN(col)) {
    return ContentService.createTextOutput(JSON.stringify({
      "status": "error",
      "message": "Parameter tidak lengkap: sheetName=" + sheetName + ", row=" + row + ", col=" + col
    })).setMimeType(ContentService.MimeType.JSON);
  }
  
  var weeklyMeetingSpreadsheetId = "1ydmss-ADSeJpw7KJyQzT44RUNaqu5wJ0UJrIxn_8EmY";
  var ss = SpreadsheetApp.openById(weeklyMeetingSpreadsheetId);
  var sheet = findSheetByFlexibleName(ss, sheetName);
  
  if (!sheet) {
    return ContentService.createTextOutput(JSON.stringify({
      "status": "error",
      "message": "Sheet '" + sheetName + "' tidak ditemukan."
    })).setMimeType(ContentService.MimeType.JSON);
  }
  
  // Kolom Jadwal Posting = colIndex + 3 (0-based: ID=col, Keterangan=col+1, PostingIG=col+2, JadwalPosting=col+3)
  var jadwalCol = col + 3;
  sheet.getRange(row, jadwalCol).setValue(jadwalPosting);
  SpreadsheetApp.flush();
  
  return ContentService.createTextOutput(JSON.stringify({
    "status": "success",
    "message": "Berhasil mengubah jadwal posting menjadi: " + jadwalPosting
  })).setMimeType(ContentService.MimeType.JSON);
}

function findAbsensiSheet(ss) {
  var sheet = ss.getSheetByName("Absensi Meeting 2026");
  if (sheet) return sheet;
  sheet = ss.getSheetByName("Absensi Meeting " + new Date().getFullYear());
  if (sheet) return sheet;
  var sheets = ss.getSheets();
  for (var i = 0; i < sheets.length; i++) {
    if (sheets[i].getName().indexOf("Absensi") !== -1) {
      return sheets[i];
    }
  }
  return null;
}

function getAbsensiMeeting(e) {
  var ssId = "1ydmss-ADSeJpw7KJyQzT44RUNaqu5wJ0UJrIxn_8EmY";
  var ss = SpreadsheetApp.openById(ssId);
  var sheet = findAbsensiSheet(ss);
  
  if (!sheet) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: "Sheet 'Absensi Meeting 2026' atau sheet absensi lainnya tidak ditemukan."
    })).setMimeType(ContentService.MimeType.JSON);
  }
  
  var monthIndex = 5;
  if (e && e.parameter && e.parameter.monthIndex !== undefined) {
    monthIndex = parseInt(e.parameter.monthIndex, 10);
  }
  
  var configs = [
    { start: 4, end: 7, total: 8, label: "Januari" },
    { start: 10, end: 13, total: 14, label: "Februari" },
    { start: 16, end: 20, total: 21, label: "Maret" },
    { start: 23, end: 26, total: 27, label: "April" },
    { start: 29, end: 32, total: 33, label: "Mei" },
    { start: 35, end: 39, total: 40, label: "Juni" },
    { start: 42, end: 45, total: 46, label: "Juli" },
    { start: 48, end: 51, total: 52, label: "Agustus" },
    { start: 54, end: 58, total: 59, label: "September" },
    { start: 61, end: 64, total: 65, label: "Oktober" },
    { start: 67, end: 70, total: 71, label: "November" },
    { start: 73, end: 77, total: 78, label: "Desember" }
  ];
  
  var config = configs[monthIndex] || configs[5];
  var startCol = config.start;
  var endCol = config.end;
  var totalCol = config.total;
  var numCols = endCol - startCol + 1;
  
  var namesRange = sheet.getRange("B6:B41");
  var namesValues = namesRange.getValues();
  
  var headerRange = sheet.getRange(4, startCol, 2, numCols);
  var headerValues = headerRange.getValues();
  
  var dates = [];
  for (var c = 0; c < numCols; c++) {
    var val4 = headerValues[0][c];
    var val5 = headerValues[1][c];
    var dateLabel = "";
    
    if (val5 instanceof Date) {
      dateLabel = formatDateDMYHelper(val5);
    } else if (val5 && val5.toString().trim() !== "") {
      dateLabel = val5.toString().trim();
    } else if (val4 instanceof Date) {
      dateLabel = formatDateDMYHelper(val4);
    } else if (val4 && val4.toString().trim() !== "") {
      dateLabel = val4.toString().trim();
    } else {
      dateLabel = "Meeting " + (c + 1);
    }
    dates.push({
      colIndex: startCol + c,
      label: dateLabel
    });
  }
  
  var dataRange = sheet.getRange(6, startCol, 36, numCols + 1);
  var dataValues = dataRange.getValues();
  
  var totalDateRange = sheet.getRange(47, startCol, 1, numCols);
  var totalDateValues = totalDateRange.getValues();
  
  var marketingList = [];
  for (var i = 0; i < namesValues.length; i++) {
    var name = namesValues[i][0] ? namesValues[i][0].toString().trim() : "";
    if (name === "") continue;
    
    var rowData = dataValues[i];
    var attendance = [];
    for (var c = 0; c < numCols; c++) {
      var val = rowData[c];
      var present = (val === true || val.toString().toLowerCase() === "true" || val === 1 || val.toString() === "v" || val.toString() === "✔");
      attendance.push(present);
    }
    
    var totalHadirBulan = rowData[numCols] ? parseInt(rowData[numCols], 10) : 0;
    
    marketingList.push({
      row: 6 + i,
      name: name,
      attendance: attendance,
      totalHadirBulan: totalHadirBulan
    });
  }
  
  var dateTotals = [];
  for (var c = 0; c < numCols; c++) {
    dateTotals.push(totalDateValues[0][c] ? parseInt(totalDateValues[0][c], 10) : 0);
  }
  
  return ContentService.createTextOutput(JSON.stringify({
    status: "success",
    monthIndex: monthIndex,
    dates: dates,
    marketingList: marketingList,
    dateTotals: dateTotals
  })).setMimeType(ContentService.MimeType.JSON);
}

function updateAbsensiMeeting(data) {
  var ssId = "1ydmss-ADSeJpw7KJyQzT44RUNaqu5wJ0UJrIxn_8EmY";
  var ss = SpreadsheetApp.openById(ssId);
  var sheet = findAbsensiSheet(ss);
  
  if (!sheet) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: "Sheet 'Absensi Meeting 2026' atau sheet absensi lainnya tidak ditemukan."
    })).setMimeType(ContentService.MimeType.JSON);
  }
  
  var row = parseInt(data.row, 10);
  var col = parseInt(data.col, 10);
  var present = (data.present === "true" || data.present === true);
  
  if (isNaN(row) || isNaN(col) || row < 6 || row > 41 || col < 4) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: "Parameter row (" + row + ") atau col (" + col + ") tidak valid."
    })).setMimeType(ContentService.MimeType.JSON);
  }
  
  var cell = sheet.getRange(row, col);
  cell.setDataValidation(SpreadsheetApp.newDataValidation().requireCheckbox().build());
  cell.setValue(present);
  
  SpreadsheetApp.flush();
  
  var configs = [
    { start: 4, end: 7, total: 8, label: "Januari" },
    { start: 10, end: 13, total: 14, label: "Februari" },
    { start: 16, end: 20, total: 21, label: "Maret" },
    { start: 23, end: 26, total: 27, label: "April" },
    { start: 29, end: 32, total: 33, label: "Mei" },
    { start: 35, end: 39, total: 40, label: "Juni" },
    { start: 42, end: 45, total: 46, label: "Juli" },
    { start: 48, end: 51, total: 52, label: "Agustus" },
    { start: 54, end: 58, total: 59, label: "September" },
    { start: 61, end: 64, total: 65, label: "Oktober" },
    { start: 67, end: 70, total: 71, label: "November" },
    { start: 73, end: 77, total: 78, label: "Desember" }
  ];
  
  var totalCol = 8; // default fallback
  for (var i = 0; i < configs.length; i++) {
    if (col >= configs[i].start && col <= configs[i].end) {
      totalCol = configs[i].total;
      break;
    }
  }
  
  var newRowTotal = sheet.getRange(row, totalCol).getValue();
  var newColTotal = sheet.getRange(47, col).getValue();
  
  return ContentService.createTextOutput(JSON.stringify({
    status: "success",
    message: "Absensi berhasil diupdate.",
    newRowTotal: newRowTotal,
    newColTotal: newColTotal
  })).setMimeType(ContentService.MimeType.JSON);
}

function getYearlyIgPostingHistory(e) {
  var weeklyMeetingSpreadsheetId = "1ydmss-ADSeJpw7KJyQzT44RUNaqu5wJ0UJrIxn_8EmY";
  var ss = SpreadsheetApp.openById(weeklyMeetingSpreadsheetId);
  var months = ["Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember"];
  
  var historyMap = {};
  
  for (var m = 0; m < months.length; m++) {
    var sheetName = "Recap Meeting " + months[m];
    var sheet = findSheetByFlexibleName(ss, sheetName);
    if (!sheet) continue;
    
    var sName = sheet.getName();
    var yearMatch = sName.match(/\b(\d{4})\b/);
    var sheetYear = yearMatch ? parseInt(yearMatch[1], 10) : 2026;
    
    var monthNum = m + 1;
    
    // === BATCH READ: baca seluruh sheet sekaligus (1 API call per sheet) ===
    var lastCol = sheet.getLastColumn();
    var maxRow = 120;
    if (lastCol < 58) lastCol = 58;
    if (lastCol > sheet.getLastColumn()) lastCol = sheet.getLastColumn();
    if (maxRow > sheet.getLastRow()) maxRow = sheet.getLastRow();
    if (maxRow <= 0 || lastCol <= 0) continue;
    
    var allValues = sheet.getRange(1, 1, maxRow, lastCol).getValues();
    
    // Deteksi kolom-kolom meeting (col 2, 10, 18, 26, 34, 42, 50, 58)
    var checkCols = [2, 10, 18, 26, 34, 42, 50, 58];
    var dateCols = [];
    
    for (var i = 0; i < checkCols.length; i++) {
      var col = checkCols[i];
      if (col > lastCol) break;
      var colIdx = col - 1; // 0-based
      
      // Deteksi tanggal dari header rows 1-4
      var foundDay = null;
      var foundDayName = "Selasa";
      for (var r = 0; r < 4; r++) {
        var val = allValues[r] ? allValues[r][colIdx] : null;
        if (!val && val !== 0) continue;
        if (val instanceof Date) {
          foundDay = val.getDate();
          foundDayName = getIndonesianDayName(val.getDay());
          break;
        }
        var valStr = val.toString().toLowerCase();
        var numMatch = valStr.match(/\b(\d{1,2})\b/);
        if (numMatch) {
          foundDay = parseInt(numMatch[1], 10);
          break;
        }
      }
      
      if (foundDay !== null) {
        dateCols.push({ colIdx: colIdx, day: foundDay, dayName: foundDayName });
      } else {
        // Fallback: hitung hari Selasa untuk bulan ini
        var tuesdays = [];
        var d = new Date(sheetYear, m, 1);
        while (d.getDay() !== 2) { d.setDate(d.getDate() + 1); }
        while (d.getMonth() === m) {
          tuesdays.push(d.getDate());
          d.setDate(d.getDate() + 7);
        }
        if (i < tuesdays.length) {
          dateCols.push({ colIdx: colIdx, day: tuesdays[i], dayName: "Selasa" });
        }
      }
    }
    
    // Proses tiap kolom meeting
    for (var d = 0; d < dateCols.length; d++) {
      var colInfo = dateCols[d];
      var colIdx0 = colInfo.colIdx; // 0-based index kolom ID Listing
      var day = colInfo.day;
      var dayName = colInfo.dayName;
      var formattedDate = dayName + ", " + day + " " + months[m] + " " + sheetYear;
      
      // Baris data mulai dari row 5 (index 4)
      for (var r = 4; r < maxRow; r++) {
        var row = allValues[r];
        if (!row) continue;
        
        var idListing = row[colIdx0] ? row[colIdx0].toString().trim() : "";
        if (idListing === "") continue;
        
        // Bersihkan trailing ".0" (dari number formatting spreadsheet)
        if (idListing.endsWith(".0")) {
          idListing = idListing.substring(0, idListing.length - 2);
        }
        
        // Kolom Posting IG = colIdx0 + 2 (0-based)
        var igColIdx = colIdx0 + 2;
        var igRaw = igColIdx < lastCol ? row[igColIdx] : null;
        
        var isDone = false;
        if (igRaw !== null && igRaw !== undefined && igRaw !== "") {
          if (typeof igRaw === "boolean") {
            isDone = igRaw === true;
          } else if (typeof igRaw === "number") {
            isDone = igRaw === 1;
          } else {
            var igStr = igRaw.toString().trim().toLowerCase();
            isDone = igStr === "true" || igStr === "done" || igStr === "ya" ||
                     igStr === "yes" || igStr === "1" || igStr === "v" ||
                     igStr === "√" || igStr === "✔";
          }
        }
        
        if (isDone) {
          // Ambil tanggal jadwal posting jika ada (kolom colIdx0 + 3)
          var jadwalColIdx = colIdx0 + 3;
          var jadwalVal = jadwalColIdx < lastCol ? row[jadwalColIdx] : null;
          var dateToRecord = formattedDate;
          if (jadwalVal && jadwalVal !== "") {
            if (jadwalVal instanceof Date) {
              var jDay = jadwalVal.getDate();
              var jMonth = jadwalVal.getMonth();
              var jYear = jadwalVal.getFullYear();
              var jDayName = getIndonesianDayName(jadwalVal.getDay());
              dateToRecord = jDayName + ", " + jDay + " " + months[jMonth] + " " + jYear;
            } else {
              var jadwalStr = jadwalVal.toString().trim();
              if (jadwalStr !== "") dateToRecord = jadwalStr;
            }
          }
          
          if (!historyMap[idListing]) {
            historyMap[idListing] = [];
          }
          // Hindari duplikat tanggal yang sama
          if (historyMap[idListing].indexOf(dateToRecord) === -1) {
            historyMap[idListing].push(dateToRecord);
          }
        }
      }
    }
  }
  
  return ContentService.createTextOutput(JSON.stringify({
    "status": "success",
    "history": historyMap
  })).setMimeType(ContentService.MimeType.JSON);
}

function getIndonesianDayName(dayNum) {
  var days = ["Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"];
  return days[dayNum] || "Selasa";
}
""".trimIndent()


    var currentMenu by rememberSaveable { mutableStateOf("general") } // "general", "auth", "developer"
    val isDarkThemeEnabled by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    
    val formatDoneDefault by viewModel.formatDone.collectAsStateWithLifecycle()
    val formatNotDoneDefault by viewModel.formatNotDone.collectAsStateWithLifecycle()
    
    var valDone by remember { mutableStateOf(formatDoneDefault) }
    var valNotDone by remember { mutableStateOf(formatNotDoneDefault) }

    LaunchedEffect(formatDoneDefault, formatNotDoneDefault) {
        valDone = formatDoneDefault
        valNotDone = formatNotDoneDefault
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SCHEDULE FOTO RWC",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = when (currentMenu) {
                                "general" -> "Pengaturan Aplikasi"
                                "auth" -> "Akses Terbatas PIN"
                                else -> "Opsi Developer (Sheets API)"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when (currentMenu) {
                                "general" -> onNavigateBack()
                                "auth" -> currentMenu = "general"
                                "developer" -> currentMenu = "general"
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when (currentMenu) {
                "general" -> {
                    // GENERAL SETTINGS PAGE
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 1. Theme Configuration
                        Text(
                            text = "TEMA TAMPILAN",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isDarkThemeEnabled) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Gunakan Tema Gelap",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = if (isDarkThemeEnabled) "Mode Gelap aktif (meredakan mata lelah)" else "Mode Terang aktif",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                Switch(
                                    checked = isDarkThemeEnabled,
                                    onCheckedChange = { viewModel.setDarkTheme(it) },
                                    modifier = Modifier.testTag("theme_toggle_switch")
                                )
                            }
                        }

                        if (isDarkThemeEnabled) {
                            val selectedThemeStyle by viewModel.selectedThemeStyle.collectAsStateWithLifecycle()
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "GAYA WARNA GELAP VARIATIF",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Sesuaikan skema warna asimetris untuk mode gelap:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val styleOptions = listOf(
                                            Triple("COSMIC_SLATE", "Cosmic Slate", androidx.compose.ui.graphics.Color(0xFFF59E0B)),
                                            Triple("NEON_AMETHYST", "Neon Amethyst", androidx.compose.ui.graphics.Color(0xFFD946EF)),
                                            Triple("FOREST_EMERALD", "Forest Emerald", androidx.compose.ui.graphics.Color(0xFF10B981))
                                        )
                                        styleOptions.forEach { (styleKey, styleName, indicatorColor) ->
                                            val isSelected = selectedThemeStyle == styleKey
                                            Surface(
                                                onClick = { viewModel.setThemeStyle(styleKey) },
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(
                                                    width = 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(indicatorColor, androidx.compose.foundation.shape.CircleShape)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = styleName,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            fontSize = 11.sp
                                                        ),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 1b. Autostart and Background Sync System Settings
                        Text(
                            text = "SISTEM AUTOSTART & NOTIFIKASI REAL-TIME",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Notifikasi Real-Time Latar Belakang",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Aplikasi menggunakan Background Sync Service agar pesan baru muncul real-time walau aplikasi ditutup.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                var isIgnoringBattery by remember { mutableStateOf(AutostartUtils.isIgnoringBatteryOptimizations(context)) }

                                // Update battery ignore state on resume
                                LaunchedEffect(Unit) {
                                    isIgnoringBattery = AutostartUtils.isIgnoringBatteryOptimizations(context)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Status Optimasi Baterai:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isIgnoringBattery) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            text = if (isIgnoringBattery) "Abaikan Optimasi (Disarankan)" else "Terbatasi Sistem",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isIgnoringBattery) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            AutostartUtils.openAutostartSettings(context)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Atur Mulai Otomatis (Autostart)", fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            AutostartUtils.requestIgnoreBatteryOptimizations(context)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.BatteryAlert, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Abaikan Optimasi Baterai HP", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // 2. WhatsApp Template Customization
                        Text(
                            text = "TEMPLATE PESAN WHATSAPP FOLLOW UP",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Info Tags Guide Code Block
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = "Panduan Tag Pengganti Otomatis",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = "Anda dapat meletakkan tanda kurung siku berikut di teks template agar terisi otomatis dengan data jadwal:\n\n" +
                                           "•  `{greeting}` : Selamat Pagi/Siang/Sore/Malam\n" +
                                           "•  `{honorific_name}` : Sapaan (Pak/Bu) + Nama ME\n" +
                                           "•  `{me_name}` : Nama Lengkap ME\n" +
                                           "•  `{id_listing}` : ID Listing Properti\n" +
                                           "•  `{type}` : Tipe Sesi (Foto, Video, Drone)\n" +
                                           "•  `{lokasi}` : Alamat Lengkap Properti\n" +
                                           "•  `{jadwal_tanggal}` : Hari & Tanggal Sesi\n" +
                                           "•  `{jadwal_jam}` : Jam Sesi Foto\n" +
                                           "•  `{staff}` : Nama Runner yang ditugaskan\n" +
                                           "•  `{status}` : Status (Pending, Done, etc.)\n" +
                                           "•  `{website_link}` : Link rincian website properti",
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        // Editor 1: Selesai (Done) Format
                        OutlinedTextField(
                            value = valDone,
                            onValueChange = { valDone = it },
                            label = { Text("Teks Template: Jadwal SELESAI (Done)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            placeholder = { Text("Contoh: Halo {me_name}, sesi {type} telah selesai...") }
                        )

                        // Editor 2: Belum Selesai (Pending) Format
                        OutlinedTextField(
                            value = valNotDone,
                            onValueChange = { valNotDone = it },
                            label = { Text("Teks Template: Jadwal BELUM SELESAI / Ambil Ulang") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            placeholder = { Text("Contoh: {greeting} {honorific_name}, listing berikut bisa kita ambil ulang kapan?") }
                        )

                        // Save Templates Button
                        Button(
                            onClick = {
                                viewModel.setFormatDone(valDone)
                                viewModel.setFormatNotDone(valNotDone)
                                Toast.makeText(context, "Template WhatsApp berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simpan Template WA", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()

                        // 3. Developer Options Card Row
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "MENU DEVELOPER (AKSES TERBATAS)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Bagian ini digunakan oleh tim pengembang untuk konfigurasi integrasi Google Spreadsheet API online dan utilitas cache penyimpanan lokal.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = { currentMenu = "auth" },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Buka Opsi Developer", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
                "auth" -> {
                    // Dhavid Febrian Valentino PIN authentication gate
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Lock Security",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Text(
                                    text = "Akses Terbatas",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Hanya dapat diakses oleh developer:\nDhavid Febrian Valentino",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                var pinInput by remember { mutableStateOf("") }
                                var hasError by remember { mutableStateOf(false) }

                                OutlinedTextField(
                                    value = pinInput,
                                    onValueChange = {
                                        pinInput = it
                                        hasError = false
                                    },
                                    label = { Text("Masukkan PIN / Kode Akses") },
                                    singleLine = true,
                                    isError = hasError,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (hasError) {
                                    Text(
                                        text = "Kode akses salah! Silakan masukkan PIN yang benar.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = {
                                        val cleaned = pinInput.trim().lowercase()
                                        if (cleaned == "137946" || cleaned == "dhavid" || cleaned == "085169671344" || cleaned == "sfrd" || cleaned == "valentino") {
                                            currentMenu = "developer"
                                        } else {
                                            hasError = true
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.VpnKey, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Buka Akses Setting", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { currentMenu = "general" },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Batal", fontWeight = FontWeight.Bold)
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                Text(
                                    text = "If there are any problems, please contact the developer:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )

                                Button(
                                    onClick = {
                                        try {
                                            val url = "https://wa.me/6285169671344"
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                data = android.net.Uri.parse(url)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Tidak dapat membuka WhatsApp", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF25D366),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = "Contact Developer",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Hubungi via WhatsApp", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                "developer" -> {
                    // LOCKED DEVELOPER ACTIONS ZONE
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { currentMenu = "general" },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kembali Ke Pengaturan Umum", fontWeight = FontWeight.Bold)
                        }

                        // Section 1: Connection Status info
                        if (currentUrl.isNotBlank()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = "Selesai dihubungkan",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "Terhubung dengan Lancar",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Aplikasi akan selalu mengunggah otomatis jadwal foto baru Anda ke Google Sheets secara realtime.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudQueue,
                                        contentDescription = "Belum dikonfigurasi",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "Mode Kerja Offline / Lokal",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Data disimpan di memori HP Anda. Silakan masukkan URL Google Apps Script di bawah untuk berkolaborasi online.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        // Section 2: Input URL and Spreadsheet ID
                        Text(
                            text = "Konfigurasi API Link",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("Google Apps Script Web App URL") },
                            placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_apps_script_url")
                        )

                        OutlinedTextField(
                            value = sheetIdInput,
                            onValueChange = { sheetIdInput = it },
                            label = { Text("Google Spreadsheet ID (Referensi)") },
                            placeholder = { Text("1L0ajG9dAmhisDQ7ADil5KKNRkzgXp_jdCi41-6mPkIw") },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_spreadsheet_id")
                        )

                        OutlinedTextField(
                            value = weeklyUrlInput,
                            onValueChange = { weeklyUrlInput = it },
                            label = { Text("Google Apps Script Weekly Meeting URL") },
                            placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_weekly_meeting_url")
                        )

                        // Save configuration button
                        Button(
                            onClick = {
                                viewModel.saveSettings(urlInput, sheetIdInput, weeklyUrlInput)
                                Toast.makeText(context, "Konfigurasi koneksi tersimpan!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_settings_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.SettingsBackupRestore, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Terapkan & Simpan URL", fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Section 3: Setup guide integration
                        Text(
                            text = "Panduan Pemasangan 1 Menit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Ikuti langkah berikut untuk menyambungkan Google Sheet Anda:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )

                                Text(
                                    text = "1. Buka spreadsheet Anda di laptop/komputer.\n" +
                                           "2. Di menu atas pilih Ekstensi (Extensions) -> Apps Script.\n" +
                                           "3. Hapus semua kode default, salin skrip di bawah dan tempelkan di file Code.gs.\n" +
                                           "4. Klik Simpan (Disket) lalu klik Terapkan (Deploy) -> Penerapan Baru (New Deployment).\n" +
                                           "5. Jenis penerapan: Aplikasi Web (Web App).\n" +
                                           "6. Jalankan sebagai: Saya (Me).\n" +
                                           "7. Yang memiliki akses: Siapa saja (Anyone / Everyone).\n" +
                                           "8. Klik Terapkan, berikan izin akses Google, salin URL Aplikasi Web Anda lalu tempelkan di kolom pengaturan di atas!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 22.sp
                                )
                            }
                        }

                        // Section 4: Deploys the code element + copy buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kode Google Apps Script",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Google Apps Script", appsScriptCode)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Skrip berhasil disalin ke papan klip!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("copy_script_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Icon", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Salin Skrip", fontSize = 12.sp)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = appsScriptCode,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(250.dp)
                                        .verticalScroll(rememberScrollState())
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Section 4.5: Agent Database and Contact Management
                        Text(
                            text = "Kelola Kontak & Foto Agen",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        var showAgentDialog by remember { mutableStateOf(false) }

                        Button(
                            onClick = { showAgentDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("manage_agents_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.People, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kelola Database Kontak & Foto", fontWeight = FontWeight.Bold)
                        }

                        if (showAgentDialog) {
                            ManageAgentsDialog(
                                viewModel = viewModel,
                                onDismiss = { showAgentDialog = false }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Section 5: Cache cleanup utilities
                        Text(
                            text = "Utilitas Penyimpanan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )

                        OutlinedButton(
                            onClick = {
                                viewModel.clearCache()
                                Toast.makeText(context, "Cache lokal berhasil dibersihkan!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("clear_cache_button"),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bersihkan Cache Lokal", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAgentsDialog(
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val contacts by viewModel.agentContacts.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            contacts.filter { 
                it.displayName.contains(searchQuery, ignoreCase = true) || 
                it.nameKey.contains(searchQuery, ignoreCase = true) 
            }
        }
    }

    var editingContact by remember { mutableStateOf<com.example.data.AgentContactEntity?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Database Kontak & Foto Agen", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                        )
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().testTag("search_agent_input"),
                        placeholder = { Text("Cari nama agen...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    if (filteredContacts.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada agen ditemukan.",
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredContacts, key = { it.nameKey }) { contact ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Avatar image with fallbacks
                                        val avatarUrl = contact.avatarUrl.ifBlank {
                                            "https://ui-avatars.com/api/?name=${contact.displayName.replace(" ", "+")}&background=random&color=fff&size=128&bold=true"
                                        }

                                        coil.compose.AsyncImage(
                                            model = avatarUrl,
                                            contentDescription = "Foto ${contact.displayName}",
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = contact.displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                                                Text(
                                                    text = contact.phone.ifBlank { "-" },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                                                Text(
                                                    text = if (contact.instagram.isNotBlank()) "@${contact.instagram}" else "-",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { editingContact = contact },
                                            modifier = Modifier.testTag("edit_agent_${contact.nameKey}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Kontak",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (editingContact != null) {
                    EditAgentDialog(
                        contact = editingContact!!,
                        onDismiss = { editingContact = null },
                        onSave = { updated ->
                            viewModel.updateAgentContact(updated)
                            editingContact = null
                            Toast.makeText(context, "Kontak ${updated.displayName} berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAgentDialog(
    contact: com.example.data.AgentContactEntity,
    onDismiss: () -> Unit,
    onSave: (com.example.data.AgentContactEntity) -> Unit
) {
    var phone by remember { mutableStateOf(contact.phone) }
    var email by remember { mutableStateOf(contact.email) }
    var instagram by remember { mutableStateOf(contact.instagram) }
    var avatarUrl by remember { mutableStateOf(contact.avatarUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Kontak: ${contact.displayName}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. Telepon / WhatsApp") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_agent_phone_input"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_agent_email_input"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = instagram,
                    onValueChange = { instagram = it },
                    label = { Text("Instagram Username (Tanpa @)") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_agent_ig_input"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = avatarUrl,
                    onValueChange = { avatarUrl = it },
                    label = { Text("URL Foto Profil / Avatar") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_agent_photo_input"),
                    placeholder = { Text("Masukkan URL gambar...") },
                    shape = RoundedCornerShape(8.dp)
                )
                
                if (avatarUrl.isNotBlank()) {
                    Text(
                        text = "Pratinjau Foto:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        modifier = Modifier.size(80.dp).align(Alignment.CenterHorizontally),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ) {
                        coil.compose.AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Pratinjau Foto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        contact.copy(
                            phone = phone.trim(),
                            email = email.trim(),
                            instagram = instagram.trim(),
                            avatarUrl = avatarUrl.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("save_agent_edit")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
