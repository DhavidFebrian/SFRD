package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.GeneralResponse
import com.example.network.SheetsApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class AgentInfo(
    val name: String = "",
    val phone: String = "",
    val waUrl: String = "",
    val avatarUrl: String = "",
    val email: String = "",
    val instagram: String = ""
)

data class AgentContact(
    val nameKey: String,
    val phone: String,
    val email: String,
    val instagram: String,
    val avatarUrl: String = ""
)

val AGENT_CONTACT_LIST = listOf(
    AgentContact("donny", "087877777677", "donny.raywhite@yahoo.com", "donny.raywhite", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/49c8a2a7b029496da370b2d445857ce5.jpg"),
    AgentContact("agung", "081808801688", "agung.rwcipete@gmail.com", "agungrwcipete", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/76df4404fcde4c45894f190ea993fe28.jpg"),
    AgentContact("bayu", "081287222799", "bayu.raywhite@gmail.com", "bayu.raywhite", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/ec540f8efa2b48dcb16a05d3c600e089.jpg"),
    AgentContact("dian", "08161338093", "dians.raywhite@gmail.com", "south.jakarta.home", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/9bac3dfc5d3743abaf283feb2ac66837.jpg"),
    AgentContact("dini", "081282331997", "diniwiryandoko@gmail.com", "diniraywhitecipete", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/87a6095c698846649a240cce4e691540.jpg"),
    AgentContact("dutta", "081381564918", "duta.raywhite@gmail.com", "duttaraywhite", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/11b73e232d75482fbdfe62cd2b29fc49.jpg"),
    AgentContact("duta", "081381564918", "duta.raywhite@gmail.com", "duttaraywhite", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/11b73e232d75482fbdfe62cd2b29fc49.jpg"),
    AgentContact("ifa", "087885588897", "ifadebrianti.raywhite@gmail.com", "ifa.raywhite", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/5174239cff7c4ebea82af33cfde98741.png"),
    AgentContact("ike", "081808361616", "ikejuliastuti@gmail.com", "raywhite_ike", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/83a185c1ac224fafb0222c8fbbcbf206.jpg"),
    AgentContact("ilham", "08561103735", "ilham.rwcipete@gmail.com", "ilhamsraywhite", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/eed74efba0244fa684931eec1fde6d25.jpg"),
    AgentContact("imelda", "082177888816", "imelda.djuarta@yahoo.com", "raywhite_imelda_djuarta", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/bf4d3155941c4dfd9f2b11c6af769185.jpg"),
    AgentContact("iskandar", "08111823456", "iskandar.raywhite@gmail.com", "iskandar.raywhite", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/08282f939a1345fc8da6f430d3a55c9c.png"),
    AgentContact("michael", "081286960275", "michael.rwcipete@gmail.com", "michael.raywhite", ""),
    AgentContact("remmy", "081286960275", "michael.rwcipete@gmail.com", "michael.raywhite", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/01fe33087b2c46deb3d0c118892a830e.jpg"),
    AgentContact("resmi", "081380620625", "resmi.raywhite@gmail.com", "", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/d0f6229f97b44b5391a670a49b491976.jpg"),
    AgentContact("sam", "081298070006", "sam.rwcipete@gmail.com", "samsuperagent", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/b56e268949314c07836bdc7c147b6097.png"),
    AgentContact("santiaji", "085219698553", "ajisantiaji88@gmail.com", "", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/fa937a9a751f47f185d1f5f6751992a8.jpg"),
    AgentContact("vincent", "081212892189", "vincent@raywhitecipete.net", "vincentbrata", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/77f71d4c791a430a9064cbee1a095441.jpg"),
    AgentContact("yayan", "082114005670", "yayanhb2005@gmail.com", "raywhite_yayan", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/6de32af7a6ae4661ad21dd78f23fa8da.jpg"),
    AgentContact("haryadi", "08111373777", "haryadi.raywhite@gmail.com", "Haryadi.raywhite", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/8e79bb1daff74ba587f0eadf26c9b399.png"),
    AgentContact("rony", "0811190046", "rony.raywhitecipete@gmail.com", "rony.raywhitecipete", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/8fdc71d7862e4d7ba439058003ea91fc.jpg"),
    AgentContact("indah", "082125120021", "indah.raywhitecipete@gmail.com", "indah.raywhite", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/0cce2d67e6a4463a877d9a5d537db1ac.jpg"),
    AgentContact("dasep", "0818348046", "dasep.raywhite@gmail.com", "dasepraywhite", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/646f72049451468f8b490cb95ea6ee70.jpg"),
    AgentContact("ruby", "085779153217", "ruby.raywhite@gmail.com", "ruby.raywhite", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/018e09bb35ba43139740c8c884d642cb.jpg"),
    AgentContact("aii dyana", "081295951179", "aiidyana.raywhite@gmail.com", "aiiraywhite", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/a53a253345c040439738eb76a499b1c4.jpg"),
    AgentContact("dyana", "081295951179", "aiidyana.raywhite@gmail.com", "aiiraywhite", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/a53a253345c040439738eb76a499b1c4.jpg"),
    AgentContact("dedie", "082198909493", "dedie.rwc@gmail.com", "dedie.raywhite", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/78637630471c44ba8bdb1a0c2b9e3efa.jpg"),
    AgentContact("ayu", "087880476149", "ayu.raywhite@gmail.com", "ayukusumarwc", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/b9c2d6de3e2e4d338284fe03b44e3a74.png"),
    AgentContact("dhenis", "08567773081", "dhenisemanuelraywhite@gmail.com", "rumahbydhenis", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/eef9c4899573431cb48f95375b0473e8.jpg"),
    AgentContact("zulkifli", "081230016702", "zulkifli.raywhite@gmail.com", "zulkifli_rwc", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/10c0124d11784da5ac97adbbc9389f1d.png"),
    AgentContact("muljadi", "0811108308", "muljadi.raywhite@yahoo.com", "", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/000d0049606e4c89b84772191e14653e.png"),
    AgentContact("andika", "081932899091", "tan.andika@gmail.com", "andika.rwc", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/73ef1fad95914d1a8f4549956aeedb0e.png"),
    AgentContact("mari", "08118087908", "marihariadi.raywhite@gmail.com", "mari.raywhite", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/c6345748eb234604b17f76d586d983e0.jpg"),
    AgentContact("amelia", "087784882233", "amelia.raywhitecipete@gmail.com", "aimeeworks.property", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/0826299b862b402ab26a998d64fcb823.png"),
    AgentContact("hilda", "0817216161", "hilda.raywhite@gmail.com", "hilda.raywhite", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/a3847789d43e4a089f2b960e4d644fb9.png"),
    AgentContact("desy", "081284001033", "desy.raywhite@gmail.com", "desy.raywhite", ""),
    AgentContact("briand", "08176676276", "briandrwc@gmail.com", "briandproperty", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/e7be7e7020494be9b88a1cb4d48d3a9e.jpg"),
    AgentContact("rika", "081218280096", "rikaraywhite@yahoo.com", "rikaraywhite1", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/635af76844b94cd2acd17594aa5e6395.jpg"),
    AgentContact("meisi", "0817855005", "meisiraywhite@gmail.com", "meisi.raywhite", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/d40e1d5178fd46278e6415a2b7f6ecd9.jpg"),
    AgentContact("yuma", "08118585137", "yumaray.raywhite@gmail.com", "yuma_ray_cht_ci", "https://s3-ap-southeast-1.amazonaws.com/hhproperty/MarketingAgent/a706d7d815874f94bffb8e21156b2d62.jpeg")
)

@Volatile
var cachedAgentContactsList: List<com.example.data.AgentContactEntity> = emptyList()

fun findContact(meName: String): AgentContact? {
    val cleanName = meName.trim().lowercase()
    if (cleanName.isBlank()) return null
    
    // 0. Try local database cache list first
    val localList = cachedAgentContactsList
    if (localList.isNotEmpty()) {
        val dbMatch = localList.find { it.nameKey == cleanName }
        if (dbMatch != null) {
            return AgentContact(dbMatch.nameKey, dbMatch.phone, dbMatch.email, dbMatch.instagram)
        }
        
        val words = cleanName.split("\\s+".toRegex())
        val wordMatch = localList.find { words.contains(it.nameKey) }
        if (wordMatch != null) {
            return AgentContact(wordMatch.nameKey, wordMatch.phone, wordMatch.email, wordMatch.instagram)
        }
        
        val fallbackMatch = localList.find { contact ->
            val isOverlap = (contact.nameKey == "ayu" && cleanName.contains("bayu")) ||
                            (contact.nameKey == "bayu" && cleanName.contains("ayu") && !cleanName.contains("bayu"))
            if (isOverlap) {
                false
            } else {
                cleanName.contains(contact.nameKey) || contact.nameKey.contains(cleanName)
            }
        }
        if (fallbackMatch != null) {
            return AgentContact(fallbackMatch.nameKey, fallbackMatch.phone, fallbackMatch.email, fallbackMatch.instagram)
        }
    }
    
    // 1. Try exact match first
    val exactMatch = AGENT_CONTACT_LIST.find { contact ->
        cleanName == contact.nameKey
    }
    if (exactMatch != null) return exactMatch
    
    // 2. Try word-based matching (this prevents "bayu" matching "ayu" or vice versa)
    val words = cleanName.split("\\s+".toRegex())
    val wordMatch = AGENT_CONTACT_LIST.find { contact ->
        words.contains(contact.nameKey)
    }
    if (wordMatch != null) return wordMatch

    // 3. Try fallback matches, but specifically prevent "ayu" and "bayu" overlap
    return AGENT_CONTACT_LIST.find { contact ->
        val isOverlap = (contact.nameKey == "ayu" && cleanName.contains("bayu")) ||
                        (contact.nameKey == "bayu" && cleanName.contains("ayu") && !cleanName.contains("bayu"))
        if (isOverlap) {
            false
        } else {
            cleanName.contains(contact.nameKey) || contact.nameKey.contains(cleanName)
        }
    }
}

fun isValidAgentName(txt: String): Boolean {
    if (txt.isBlank()) return false
    val lower = txt.lowercase()
    if (lower.contains("marketing") || 
        lower.contains("ray white") || 
        lower.contains("cipete") || 
        lower.contains("admin") || 
        lower.contains("phone") || 
        lower.contains("whatsapp") || 
        lower.contains("email") || 
        lower.contains("contact") ||
        lower.contains("detail") ||
        lower.contains("property") ||
        lower.contains("listing") ||
        lower.contains("alamat") ||
        lower.contains("lokasi") ||
        lower.contains("hubungi") ||
        lower.contains("rp") ||
        lower.contains("milyar") ||
        lower.contains("juta") ||
        lower.contains("sales") ||
        lower.contains("office")) return false
    
    // Check if it's mostly digits or symbols
    if (txt.replace(Regex("[^0-9]"), "").length >= 5) return false
    if (txt.length < 2 || txt.length > 45) return false
    
    return true
}

fun capitalizeName(name: String): String {
    return name.split("\\s+".toRegex()).joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale("id", "ID")) else it.toString() }
    }
}

fun formatAgentName(scrapedName: String): String {
    val parts = scrapedName.split("/").map { it.trim() }.filter { it.isNotBlank() }
    val formattedParts = parts.map { part ->
        val contact = findContact(part)
        if (contact != null) {
            capitalizeName(contact.nameKey)
        } else {
            capitalizeName(part)
        }
    }
    return formattedParts.joinToString(" / ")
}

fun parseMultipleAgentNames(rawNames: String): List<String> {
    if (rawNames.isBlank()) return emptyList()
    val normalized = rawNames
        .replace(" dan ", "/")
        .replace(" Dan ", "/")
        .replace(" DAN ", "/")
        .replace(" & ", "/")
        .replace(",", "/")
        .replace(";", "/")
    return normalized.split("/").map { it.trim() }.filter { it.isNotBlank() }
}

fun getAgentPhoneByName(meName: String): String {
    val firstPart = meName.split("/").firstOrNull()?.trim() ?: meName
    val contact = findContact(firstPart)
    return contact?.phone ?: ""
}

fun getAgentNameByPhone(phone: String): String {
    val cleanPhone = phone.replace("+", "").replace("-", "").replace(" ", "").removePrefix("62").removePrefix("0")
    if (cleanPhone.isBlank()) return ""
    val contact = AGENT_CONTACT_LIST.find { contact ->
        val contactClean = contact.phone.replace("+", "").replace("-", "").replace(" ", "").removePrefix("62").removePrefix("0")
        contactClean == cleanPhone || (contactClean.length >= 7 && cleanPhone.endsWith(contactClean)) || (cleanPhone.length >= 7 && contactClean.endsWith(cleanPhone))
    }
    return contact?.nameKey?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale("id", "ID")) else it.toString() } ?: ""
}

fun getAgentEmailByName(meName: String): String {
    val firstPart = meName.split("/").firstOrNull()?.trim() ?: meName
    val contact = findContact(firstPart)
    return contact?.email ?: ""
}

fun getAgentInstagramByName(meName: String): String {
    val firstPart = meName.split("/").firstOrNull()?.trim() ?: meName
    val contact = findContact(firstPart)
    if (contact != null && contact.instagram.isNotBlank()) {
        val ig = contact.instagram
        return if (ig.startsWith("@")) ig else "@$ig"
    }
    return ""
}

fun cleanListingDescription(rawDesc: String): String {
    val lines = rawDesc.split("\n")
    var numLineIndex = -1
    // Look at the top 8 lines for a line containing only digits
    for (i in 0 until minOf(lines.size, 8)) {
        val trimmedLine = lines[i].trim()
        if (trimmedLine.isNotEmpty() && trimmedLine.matches("""\d+""".toRegex())) {
            numLineIndex = i
            break
        }
    }
    if (numLineIndex != -1 && numLineIndex < lines.size - 1) {
        // Drop the number line and everything above it, and only return lines below it!
        return lines.drop(numLineIndex + 1).joinToString("\n").trim()
    }
    return rawDesc.trim()
}

fun getCleanTextWithNewlines(node: org.jsoup.nodes.Node): String {
    val sb = java.lang.StringBuilder()
    fun traverse(n: org.jsoup.nodes.Node) {
        if (n is org.jsoup.nodes.TextNode) {
            sb.append(n.text())
        } else if (n is org.jsoup.nodes.Element) {
            val tagName = n.tagName()
            if (tagName == "br") {
                sb.append("\n")
            } else if (tagName == "p" || tagName == "div" || tagName == "li" || tagName == "h5" || tagName == "h4" || tagName == "h3") {
                if (sb.isNotEmpty() && !sb.endsWith("\n")) {
                    sb.append("\n")
                }
                for (child in n.childNodes()) {
                    traverse(child)
                }
                if (!sb.endsWith("\n")) {
                    sb.append("\n")
                }
            } else {
                for (child in n.childNodes()) {
                    traverse(child)
                }
            }
        }
    }
    traverse(node)
    return sb.toString()
}

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val preferenceManager = PreferenceManager(application)
    
    // Set up Retrofit dynamically
    private val apiService: SheetsApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://script.google.com/") // Placeholder, URL bypassed by @Url
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SheetsApiService::class.java)
    }

    private val repository = ScheduleRepository(db.scheduleDao(), db.editFotoTaskDao(), apiService, preferenceManager)

    // Agent Contacts Database Integration
    val agentContacts: StateFlow<List<AgentContactEntity>> = db.agentContactDao().getAllAgentContactsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateAgentContact(contact: AgentContactEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            db.agentContactDao().insertAgentContact(contact)
        }
    }

    fun deleteAgentContact(nameKey: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            db.agentContactDao().deleteAgentContact(nameKey)
        }
    }

    private fun capitalizeName(name: String): String {
        if (name.isBlank()) return ""
        return name.split(" ").joinToString(" ") { 
            it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() } 
        }
    }

    // Chat Feature Integration
    val chatMessages: StateFlow<List<ChatMessage>> = db.chatMessageDao().getAllMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val visibleChatMessages: StateFlow<List<ChatMessage>> = chatMessages
        .map { list -> list.filter { !it.message.startsWith("READ_RECEIPT_FOR_") } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userLastReadTimestamps: StateFlow<Map<String, Long>> = chatMessages
        .map { list ->
            list.filter { it.message.startsWith("READ_RECEIPT_FOR_") }
                .associate { msg ->
                    val user = msg.message.removePrefix("READ_RECEIPT_FOR_")
                    user to msg.timestamp
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun updateMyReadReceipt(latestTimestamp: Long) {
        val sender = getSenderName()
        val key = "READ_RECEIPT_FOR_$sender"
        viewModelScope.launch {
            val existing = chatMessages.value.find { it.message == key }
            val isEdit = existing != null
            val no = existing?.no ?: 0
            
            try {
                val url = getChatAppsScriptUrl()
                if (url.isNotBlank()) {
                    val sdfDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    val sdfTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                    val dateStr = sdfDate.format(java.util.Date(latestTimestamp))
                    val timeStr = sdfTime.format(java.util.Date(latestTimestamp))

                    val sheetModel = com.example.network.SheetSchedule(
                        no = if (isEdit) no else 0,
                        idListing = key,
                        namaMe = sender,
                        lokasi = "CHAT_MESSAGE",
                        tanggal = dateStr,
                        jam = timeStr,
                        staff = android.os.Build.MODEL ?: "Unknown",
                        type = latestTimestamp.toString(),
                        status = "active",
                        action = if (isEdit) "edit" else "add",
                        sheetName = "Chat"
                    )
                    val result = apiService.addSchedule(url, sheetModel)
                    val statusLow = result.status.lowercase()
                    val msgLow = result.message.lowercase()
                    if (statusLow == "success" || statusLow == "ok" || msgLow == "success" || msgLow == "ok") {
                        val remoteNo = if (!isEdit && result.row != null && result.row > 4) result.row - 4 else no
                        db.chatMessageDao().insertMessage(
                            ChatMessage(
                                id = existing?.id ?: 0,
                                no = remoteNo,
                                senderName = sender,
                                message = key,
                                timestamp = latestTimestamp,
                                deviceModel = android.os.Build.MODEL ?: "Unknown",
                                synced = true
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private val _chatSyncing = MutableStateFlow(false)
    val chatSyncing: StateFlow<Boolean> = _chatSyncing.asStateFlow()

    private val _isChatSheetMissing = MutableStateFlow(false)
    val isChatSheetMissing: StateFlow<Boolean> = _isChatSheetMissing.asStateFlow()

    val isInChatScreen = MutableStateFlow(false)
    private val _unreadChatCount = MutableStateFlow(0)
    val unreadChatCount: StateFlow<Int> = _unreadChatCount.asStateFlow()

    private fun setupChatNotificationsAndBadges() {
        var lastSeen = preferenceManager.lastSeenChatTimestamp
        var lastNotifiedTimestamp = lastSeen
        var lastSentReadReceipt = 0L

        viewModelScope.launch {
            combine(chatMessages, isInChatScreen) { messages, inChat ->
                Pair(messages, inChat)
            }.collect { (messages, inChat) ->
                val currentSender = getSenderName()
                val visibleMessages = messages.filter { !it.message.startsWith("READ_RECEIPT_FOR_") }
                
                if (lastSeen == 0L && visibleMessages.isNotEmpty()) {
                    val maxTs = visibleMessages.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis()
                    preferenceManager.lastSeenChatTimestamp = maxTs
                    lastSeen = maxTs
                    lastNotifiedTimestamp = maxTs
                } else if (lastSeen == 0L) {
                    val now = System.currentTimeMillis()
                    preferenceManager.lastSeenChatTimestamp = now
                    lastSeen = now
                    lastNotifiedTimestamp = now
                }

                if (inChat) {
                    val maxTs = visibleMessages.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis()
                    if (maxTs > lastSeen) {
                        preferenceManager.lastSeenChatTimestamp = maxTs
                        lastSeen = maxTs
                    }
                    if (maxTs > lastSentReadReceipt) {
                        lastSentReadReceipt = maxTs
                        updateMyReadReceipt(maxTs)
                    }
                    if (maxTs > lastNotifiedTimestamp) {
                        lastNotifiedTimestamp = maxTs
                    }
                    _unreadChatCount.value = 0
                    dismissChatNotifications()
                } else {
                    val count = visibleMessages.count { it.timestamp > lastSeen && it.senderName != currentSender }
                    _unreadChatCount.value = count

                    val newMessages = visibleMessages.filter { it.timestamp > lastNotifiedTimestamp && it.senderName != currentSender }
                    if (newMessages.isNotEmpty()) {
                        newMessages.sortedBy { it.timestamp }.forEach { msg ->
                            showChatNotification(msg)
                        }
                        val maxTs = newMessages.maxOfOrNull { it.timestamp } ?: lastNotifiedTimestamp
                        lastNotifiedTimestamp = maxTs
                    }
                }
            }
        }
    }

    private fun showChatNotification(message: ChatMessage) {
        val context = getApplication<Application>().applicationContext
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "chat_messages_channel"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Pesan Chat Baru",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi untuk pesan obrolan tim baru"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val clickIntent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to_chat", true)
        }
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        } else {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = android.app.PendingIntent.getActivity(context, 9999, clickIntent, flags)

        val sender = message.senderName
        val cleanMsg = if (message.message.startsWith("[ATTACH:")) {
            val idx = message.message.indexOf("]")
            if (idx != -1) {
                "[Lampiran] " + message.message.substring(idx + 1).trim()
            } else {
                "[Lampiran] " + message.message
            }
        } else {
            message.message
        }

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pesan dari $sender")
            .setContentText(cleanMsg)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(cleanMsg))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(7777, notification)
    }

    private fun dismissChatNotifications() {
        val context = getApplication<Application>().applicationContext
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(7777)
    }

    // Form inputs state
    var formListingId = MutableStateFlow("")
    var formNamaMe = MutableStateFlow("")
    var formLokasi = MutableStateFlow("")
    var formTanggal = MutableStateFlow("")
    var formJam = MutableStateFlow("")
    var formStaff = MutableStateFlow("")
    var formType = MutableStateFlow("Foto")
    var formStatus = MutableStateFlow("Pending")
    var editingSchedule = MutableStateFlow<Schedule?>(null)
    private var lastAutofilledId: String? = null

    // State flow for selected schedule opened from chat attachment
    val selectedScheduleFromChat = MutableStateFlow<Schedule?>(null)

    // Current category tab state shared between MainActivity and Dashboard
    var selectedCategory = MutableStateFlow(ScheduleCategory.AKTIF)

    // Filter controls state
    var searchQuery = MutableStateFlow("")
    var filterStaff = MutableStateFlow("Semua")
    var filterStartDate = MutableStateFlow<String?>(null)
    var filterEndDate = MutableStateFlow<String?>(null)

    // Settings inputs
    var appsScriptUrl = MutableStateFlow(preferenceManager.appsScriptUrl)
    var weeklyMeetingUrl = MutableStateFlow(preferenceManager.weeklyMeetingUrl)
    var spreadsheetId = MutableStateFlow(preferenceManager.spreadsheetId)

    var isDarkTheme = MutableStateFlow(preferenceManager.isDarkTheme)
    var selectedThemeStyle = MutableStateFlow(preferenceManager.selectedThemeStyle)
    var formatDone = MutableStateFlow(preferenceManager.formatDone)
    var formatNotDone = MutableStateFlow(preferenceManager.formatNotDone)
    var selectedMonth = MutableStateFlow(preferenceManager.selectedMonth)
    
    // Weekly Meeting States
    val meetingListings = MutableStateFlow<List<com.example.network.MeetingListing>>(emptyList())
    val selectedMeetingDate = MutableStateFlow<String?>(null)
    val selectedMeetingMonth = MutableStateFlow<String?>(null)
    private val _meetingSyncStatus = MutableStateFlow<SyncState>(SyncState.Idle)
    val meetingSyncStatus: StateFlow<SyncState> = _meetingSyncStatus.asStateFlow()

    val weeklyMeetingIgListings = MutableStateFlow<List<com.example.network.MeetingListing>>(emptyList())
    private val _weeklyMeetingIgSyncStatus = MutableStateFlow<SyncState>(SyncState.Idle)
    val weeklyMeetingIgSyncStatus: StateFlow<SyncState> = _weeklyMeetingIgSyncStatus.asStateFlow()

    // Absensi Meeting 2026 States
    val absensiData = MutableStateFlow<com.example.network.AbsensiResponse?>(null)
    private val _absensiSyncStatus = MutableStateFlow<SyncState>(SyncState.Idle)
    val absensiSyncStatus: StateFlow<SyncState> = _absensiSyncStatus.asStateFlow()
    val selectedAbsenMonthIndex = MutableStateFlow<Int?>(null)

    fun fetchAbsensiMeeting(monthIndex: Int) {
        selectedAbsenMonthIndex.value = monthIndex
        val baseUrl = appsScriptUrl.value
        if (baseUrl.isBlank()) {
            _absensiSyncStatus.value = SyncState.Error("URL Google Apps Script belum diatur di menu Setting.")
            return
        }
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _absensiSyncStatus.value = SyncState.Loading
            try {
                val separator = if (baseUrl.contains("?")) "&" else "?"
                val url = "${baseUrl}${separator}action=get_absensi_meeting&monthIndex=$monthIndex"
                
                val response = apiService.getAbsensiMeeting(url)
                if (response.status.lowercase() == "success") {
                    absensiData.value = response
                    _absensiSyncStatus.value = SyncState.Success("Berhasil memuat absensi bulan ${getMonthName(monthIndex)}!")
                } else {
                    _absensiSyncStatus.value = SyncState.Error("Gagal: " + response.message)
                }
            } catch (e: Exception) {
                _absensiSyncStatus.value = SyncState.Error("Gagal mengambil data absensi: ${e.localizedMessage ?: "Masalah koneksi"}")
            }
        }
    }

    fun fetchAbsensiMeetingSilently(monthIndex: Int) {
        val baseUrl = appsScriptUrl.value
        if (baseUrl.isBlank()) return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val separator = if (baseUrl.contains("?")) "&" else "?"
                val url = "${baseUrl}${separator}action=get_absensi_meeting&monthIndex=$monthIndex"
                
                val response = apiService.getAbsensiMeeting(url)
                if (response.status.lowercase() == "success") {
                    if (absensiData.value != response) {
                        absensiData.value = response
                    }
                }
            } catch (e: Exception) {
                // Ignore silent refresh exceptions
            }
        }
    }

    private fun safeConvertToInt(value: Any?): Int {
        if (value == null) return 0
        if (value is Number) return value.toInt()
        val str = value.toString().trim()
        if (str.isEmpty()) return 0
        return str.toDoubleOrNull()?.toInt() ?: 0
    }

    fun updateAbsensiMeeting(
        row: Int,
        col: Int,
        present: Boolean,
        onResult: (Boolean, String, Int, Int) -> Unit
    ) {
        val baseUrl = appsScriptUrl.value
        if (baseUrl.isBlank()) {
            onResult(false, "URL Google Apps Script belum diatur di menu Setting.", 0, 0)
            return
        }
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val request = com.example.network.UpdateAbsensiRequest(
                    action = "update_absensi_meeting",
                    row = row,
                    col = col,
                    present = present
                )
                val response = apiService.updateAbsensiMeeting(baseUrl, request)
                if (response.status.lowercase() == "success") {
                    val resolvedRowTotal = safeConvertToInt(response.newRowTotal)
                    val resolvedColTotal = safeConvertToInt(response.newColTotal)
                    val current = absensiData.value
                    if (current != null) {
                        val updatedList = current.marketingList.map { m ->
                            if (m.row == row) {
                                val dateIndex = current.dates.indexOfFirst { it.colIndex == col }
                                if (dateIndex != -1) {
                                    val newAttendance = m.attendance.toMutableList()
                                    while (newAttendance.size <= dateIndex) {
                                        newAttendance.add(false)
                                    }
                                    newAttendance[dateIndex] = present
                                    m.copy(
                                        attendance = newAttendance,
                                        totalHadirBulan = resolvedRowTotal
                                    )
                                } else {
                                    m
                                }
                            } else {
                                m
                            }
                        }
                        val updatedTotals = current.dateTotals.toMutableList()
                        val colIndex = current.dates.indexOfFirst { it.colIndex == col }
                        if (colIndex != -1) {
                            while (updatedTotals.size <= colIndex) {
                                updatedTotals.add(0)
                            }
                            updatedTotals[colIndex] = resolvedColTotal
                        }
                        absensiData.value = current.copy(
                            marketingList = updatedList,
                            dateTotals = updatedTotals
                        )
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(true, "Absensi berhasil disimpan!", resolvedRowTotal, resolvedColTotal)
                    }
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(false, response.message, 0, 0)
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "Gagal menyimpan absensi: ${e.localizedMessage ?: "Koneksi terganggu"}", 0, 0)
                }
            }
        }
    }

    private fun getMonthName(monthIndex: Int): String {
        val list = listOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni", 
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        return list.getOrNull(monthIndex) ?: "Juni"
    }

    fun getWeeklyMeetingSheetNameForMonth(photoMonth: String): String {
        val cleaned = photoMonth.trim()
        if (cleaned.startsWith("Recap Meeting ", ignoreCase = true)) {
            return cleaned
        }
        return "Recap Meeting $cleaned"
    }

    fun fetchWeeklyMeetingIgListings(photoMonth: String) {
        val baseUrl = appsScriptUrl.value
        if (baseUrl.isBlank()) {
            _weeklyMeetingIgSyncStatus.value = SyncState.Error("URL Google Apps Script belum diatur di menu Setting.")
            return
        }
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _weeklyMeetingIgSyncStatus.value = SyncState.Loading
            try {
                val sheetName = getWeeklyMeetingSheetNameForMonth(photoMonth)
                val encodedSheet = java.net.URLEncoder.encode(sheetName, "UTF-8")
                val separator = if (baseUrl.contains("?")) "&" else "?"
                val url = "$baseUrl${separator}action=get_all_weekly_meeting_listings&sheetName=$encodedSheet"
                val response = try {
                    apiService.getMeetingListings(url)
                } catch (e: Exception) {
                    null
                }

                val combinedListings = mutableListOf<com.example.network.MeetingListing>()
                if (response != null && response.status.lowercase() == "success") {
                    combinedListings.addAll(response.listings)
                }
                
                val filtered = combinedListings.filter { 
                    it.keterangan.trim().equals("IG", ignoreCase = true) 
                }
                weeklyMeetingIgListings.value = filtered
                _weeklyMeetingIgSyncStatus.value = SyncState.Success("Berhasil memuat ${filtered.size} postingan IG dari sheet $sheetName!")
                fetchYearlyIgPostingHistory()
                
                filtered.forEach { listing ->
                    fetchListingImageIfNeeded(listing.idListing, listing.namaMe)
                }
            } catch (e: Exception) {
                _weeklyMeetingIgSyncStatus.value = SyncState.Error("Gagal mengambil data IG: ${e.localizedMessage ?: "Masalah koneksi"}")
            }
        }
    }

    fun updateWeeklyMeetingIgPost(
        photoMonth: String,
        dateStr: String,
        row: Int,
        colIndex: Int,
        postingIg: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        val baseUrl = appsScriptUrl.value
        if (baseUrl.isBlank()) {
            onResult(false, "URL Google Apps Script belum diatur.")
            return
        }
        val sheetName = getWeeklyMeetingSheetNameForMonth(photoMonth)
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val request = com.example.network.UpdateMeetingIgPostRequest(
                    sheetName = sheetName,
                    date = dateStr,
                    row = row,
                    colIndex = colIndex,
                    postingIg = postingIg
                )
                val response = apiService.updateMeetingIgPost(baseUrl, request)
                if (response.status.lowercase() == "success") {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(true, response.message)
                    }
                    fetchWeeklyMeetingIgListings(photoMonth)
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(false, response.message)
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "Gagal mengupdate status: ${e.localizedMessage ?: "Masalah koneksi"}")
                }
            }
        }
    }

    fun getMeetingDatesForMonth(monthIndex: Int): List<Pair<String, String>> {
        val currentYear = 2026
        val list = mutableListOf<Pair<String, String>>()
        val cal = java.util.Calendar.getInstance(java.util.Locale("id", "ID"))
        cal.set(java.util.Calendar.YEAR, currentYear)
        cal.set(java.util.Calendar.MONTH, monthIndex)
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        
        val maxDays = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        val dayFormat = java.text.SimpleDateFormat("EEEE", java.util.Locale("id", "ID"))
        val monthFormat = java.text.SimpleDateFormat("MMMM", java.util.Locale("id", "ID"))
        val mName = monthFormat.format(cal.time)
        
        for (day in 1..maxDays) {
            cal.set(java.util.Calendar.DAY_OF_MONTH, day)
            if (cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.TUESDAY) {
                val dayName = dayFormat.format(cal.time)
                list.add(day.toString() to "$dayName, $day $mName")
            } else if (currentYear == 2026 && monthIndex == 5 && day == 15) {
                val dayName = "Senin"
                list.add(day.toString() to "$dayName, $day $mName")
            }
        }
        list.sortBy { it.first.toIntOrNull() ?: 0 }
        return list
    }

    fun findRelevantDateIndex(dates: List<String>, todayStr: String): Int {
        if (dates.isEmpty()) return 0
        
        var bestIndex = -1
        for (i in dates.indices) {
            val dateVal = dates[i]
            if (dateVal <= todayStr) {
                if (bestIndex == -1 || dateVal > dates[bestIndex]) {
                    bestIndex = i
                }
            }
        }
        
        if (bestIndex != -1) {
            return bestIndex
        }
        return 0
    }

    fun selectRelevantMeetingDateAutomatically() {
        val todayCal = java.util.Calendar.getInstance()
        val currentYear = 2026
        val monthIndex = todayCal.get(java.util.Calendar.MONTH) // 0-based
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val todayStr = sdf.format(todayCal.time)
        
        val monthsList = listOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        
        val selectedMonthName = monthsList[monthIndex]
        val sheetName = "Recap Meeting $selectedMonthName"
        
        val datesList = getMeetingDatesForMonth(monthIndex)
        val monthNum = monthIndex + 1
        val monthStr = monthNum.toString().padStart(2, '0')
        val dateStrList = datesList.map { (day, _) ->
            "$currentYear-$monthStr-${day.padStart(2, '0')}"
        }
        
        if (dateStrList.isNotEmpty()) {
            val idx = findRelevantDateIndex(dateStrList, todayStr)
            val selectedDay = datesList[idx].first
            val dateStr = "$currentYear-$monthStr-${selectedDay.padStart(2, '0')}"
            
            fetchMeetingListings(sheetName, dateStr)
        } else {
            // Default fallback
            fetchMeetingListings(sheetName, "$currentYear-$monthStr-07")
        }
    }

    fun fetchMeetingListings(month: String, dateStr: String) {
        val baseUrl = appsScriptUrl.value
        if (baseUrl.isBlank()) {
            _meetingSyncStatus.value = SyncState.Error("URL Google Apps Script belum diatur di menu Setting.")
            return
        }
        
        selectedMeetingMonth.value = month
        selectedMeetingDate.value = dateStr
        meetingListings.value = emptyList() // Clear current listings while loading
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _meetingSyncStatus.value = SyncState.Loading
            try {
                val separator = if (baseUrl.contains("?")) "&" else "?"
                val encodedSheet = java.net.URLEncoder.encode(month, "UTF-8")
                val url = "$baseUrl${separator}action=get_weekly_meeting_listings&sheetName=$encodedSheet&date=$dateStr"
                
                val response = apiService.getMeetingListings(url)
                if (response.status.lowercase() == "success") {
                    meetingListings.value = response.listings
                    _meetingSyncStatus.value = SyncState.Success("Berhasil memuat ${response.listings.size} listing hasil meeting!")
                    
                    // Fetch images / details for each listing ID in background
                    response.listings.forEach { listing ->
                        fetchListingImageIfNeeded(listing.idListing, listing.namaMe)
                    }
                } else {
                    _meetingSyncStatus.value = SyncState.Error(response.message ?: "Gagal: ${response.status}")
                }
            } catch (e: Exception) {
                _meetingSyncStatus.value = SyncState.Error("Gagal mengambil data meeting: ${e.localizedMessage ?: "Masalah koneksi"}")
            }
        }
    }

    fun fetchMeetingListingsSilently(month: String, dateStr: String) {
        val baseUrl = appsScriptUrl.value
        if (baseUrl.isBlank()) return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val separator = if (baseUrl.contains("?")) "&" else "?"
                val encodedSheet = java.net.URLEncoder.encode(month, "UTF-8")
                val url = "$baseUrl${separator}action=get_weekly_meeting_listings&sheetName=$encodedSheet&date=$dateStr"
                
                val response = apiService.getMeetingListings(url)
                if (response.status.lowercase() == "success") {
                    if (meetingListings.value != response.listings) {
                        meetingListings.value = response.listings
                    }
                    response.listings.forEach { listing ->
                        fetchListingImageIfNeeded(listing.idListing, listing.namaMe)
                    }
                }
            } catch (e: Exception) {
                // Ignore silent refresh exceptions
            }
        }
    }

    fun getAutofilledMeForListingId(id: String): String {
        val normalizedId = normalizeIdListing(id.trim())
        if (normalizedId.isBlank()) return ""
        
        val sheetMe = allSchedules.value.find { 
            normalizeIdListing(it.idListing).equals(normalizedId, ignoreCase = true) && 
            it.namaMe.isNotBlank() 
        }?.namaMe?.trim()
        if (!sheetMe.isNullOrBlank()) return sheetMe

        val taskMe = allEditFotoTasks.value.find { 
            normalizeIdListing(it.idListing).equals(normalizedId, ignoreCase = true) && 
            it.namaMe.isNotBlank() 
        }?.namaMe?.trim()
        if (!taskMe.isNullOrBlank()) return taskMe

        val scrapedMe = agentInfoMap.value[id.trim()]?.name ?: agentInfoMap.value[normalizedId]?.name
        if (!scrapedMe.isNullOrBlank()) return scrapedMe

        return ""
    }

    fun addWeeklyMeetingListing(
        month: String,
        dateStr: String,
        idListing: String,
        namaMe: String,
        keterangan: String,
        catatan: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val baseUrl = appsScriptUrl.value
        if (baseUrl.isBlank()) {
            onResult(false, "URL Google Apps Script belum diatur di menu Setting.")
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val request = com.example.network.AddMeetingListingRequest(
                    sheetName = month,
                    date = dateStr,
                    idListing = idListing,
                    namaMe = namaMe,
                    keterangan = keterangan,
                    catatan = catatan
                )
                
                val response = apiService.addMeetingListing(baseUrl, request)
                if (response.status.lowercase() == "success") {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(true, response.message)
                    }
                    fetchMeetingListings(month, dateStr)
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(false, response.message)
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "Gagal menambahkan data: ${e.localizedMessage ?: "Masalah koneksi"}")
                }
            }
        }
    }

    private val _isAutoScrapingActive = MutableStateFlow(false)
    val isAutoScrapingActive: StateFlow<Boolean> = _isAutoScrapingActive.asStateFlow()

    private val _totalWebHotListings = MutableStateFlow("2.994")
    val totalWebHotListings: StateFlow<String> = _totalWebHotListings.asStateFlow()

    private val _pendingScrapedListings = MutableStateFlow<List<String>>(emptyList())
    val pendingScrapedListings: StateFlow<List<String>> = _pendingScrapedListings.asStateFlow()

    private var scrapingJob: kotlinx.coroutines.Job? = null
    private val persistentlySeenIds = mutableSetOf<String>()

    fun startAutoScraping(currentListings: List<String>) {
        if (_isAutoScrapingActive.value) return
        _isAutoScrapingActive.value = true
        
        scrapingJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            persistentlySeenIds.addAll(currentListings)
            
            while (_isAutoScrapingActive.value) {
                try {
                    val url = "https://raywhitecipete.net/"
                    val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
                        object : javax.net.ssl.X509TrustManager {
                            override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                            override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                        }
                    )
                    
                    val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
                    sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                    val sslSocketFactory = sslContext.socketFactory
                    
                    val client = okhttp3.OkHttpClient.Builder()
                        .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
                        .hostnameVerifier { _, _ -> true }
                        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .build()

                    val request = okhttp3.Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val html = response.body?.string() ?: ""
                            val doc = org.jsoup.Jsoup.parse(html)
                            
                            // Try to parse total listings count dynamically
                            try {
                                val textContent = doc.text()
                                val countRegex = """(?i)(\d{1,3}[\.,]\d{3}|\d+)\s*(?:properti|properties|listings|listing|found|ditemukan)""".toRegex()
                                val match = countRegex.find(textContent)
                                if (match != null) {
                                    val matchedStr = match.groupValues[1]
                                    val cleanNum = matchedStr.replace(".", "").replace(",", "").toIntOrNull()
                                    if (cleanNum != null) {
                                        _totalWebHotListings.value = java.text.NumberFormat.getIntegerInstance(java.util.Locale("id", "ID")).format(cleanNum)
                                    } else {
                                        _totalWebHotListings.value = matchedStr
                                    }
                                } else {
                                    val fourDigitRegex = """\b(2\d{3})\b""".toRegex()
                                    val fallbackMatch = fourDigitRegex.find(textContent)
                                    if (fallbackMatch != null) {
                                        val cleanNum = fallbackMatch.groupValues[1].toInt()
                                        _totalWebHotListings.value = java.text.NumberFormat.getIntegerInstance(java.util.Locale("id", "ID")).format(cleanNum)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            
                            val scrapedIds = mutableListOf<String>()
                            doc.select("a").forEach { a ->
                                val href = a.attr("href") ?: ""
                                if (href.contains("/ListingView/Detail/")) {
                                    val id = href.substringAfter("/ListingView/Detail/").substringBefore("/").substringBefore("?").trim()
                                    if (id.isNotBlank() && id.all { it.isLetterOrDigit() } && !scrapedIds.contains(id)) {
                                        scrapedIds.add(id)
                                    }
                                }
                            }
                            
                            // Regex fallback
                            val regex = """ListingView/Detail/([a-zA-Z0-9]+)""".toRegex()
                            val regexMatches = regex.findAll(html).map { it.groupValues[1] }.toList()
                            for (id in regexMatches) {
                                if (id.isNotBlank() && !scrapedIds.contains(id)) {
                                    scrapedIds.add(id)
                                }
                            }
                            
                            // Check for any new IDs that are not in persistentlySeenIds
                            val newIds = scrapedIds.filter { it !in persistentlySeenIds }
                            if (newIds.isNotEmpty()) {
                                _pendingScrapedListings.update { currentPending ->
                                    val updated = currentPending.toMutableList()
                                    for (newId in newIds) {
                                        if (newId !in updated) {
                                            updated.add(newId)
                                            // Prefetch image & detail in background so it's loaded instantly when dialog pops up
                                            fetchListingImageIfNeeded(newId)
                                        }
                                    }
                                    updated
                                }
                                persistentlySeenIds.addAll(newIds)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // Poll every 5 seconds for fast real-time operator response
                kotlinx.coroutines.delay(5000)
            }
        }
    }

    fun stopAutoScraping() {
        _isAutoScrapingActive.value = false
        scrapingJob?.cancel()
        scrapingJob = null
    }

    fun removePendingScrapedListing(idListing: String) {
        _pendingScrapedListings.update { current ->
            current.filter { it != idListing }
        }
    }

    fun clearPendingScrapedListings() {
        _pendingScrapedListings.value = emptyList()
    }

    fun updateWeeklyMeetingDetails(
        month: String,
        dateStr: String,
        row: Int,
        colIndex: Int,
        idListing: String,
        namaMe: String,
        keterangan: String,
        catatan: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val baseUrl = appsScriptUrl.value
        if (baseUrl.isBlank()) {
            onResult(false, "URL Google Apps Script belum diatur di menu Setting.")
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val request = com.example.network.UpdateMeetingDetailsRequest(
                    sheetName = month,
                    date = dateStr,
                    row = row,
                    colIndex = colIndex,
                    idListing = idListing,
                    namaMe = namaMe,
                    keterangan = keterangan,
                    catatan = catatan
                )
                
                val response = apiService.updateMeetingDetails(baseUrl, request)
                if (response.status.lowercase() == "success") {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(true, response.message)
                    }
                    fetchMeetingListings(month, dateStr)
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(false, response.message)
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "Gagal mengubah data: ${e.localizedMessage ?: "Masalah koneksi"}")
                }
            }
        }
    }

    fun getAgentAvatarByName(name: String): String? {
        if (name.isBlank()) return null
        
        // 0. Search local database cache first
        val cleanLook = name.trim().lowercase()
        val localContact = cachedAgentContactsList.find {
            it.nameKey == cleanLook || 
            it.nameKey.contains(cleanLook) || 
            cleanLook.contains(it.nameKey)
        }
        if (localContact != null && localContact.avatarUrl.isNotBlank()) {
            return localContact.avatarUrl
        }

        // 1. Search in-memory map
        _agentInfoMap.value.values.find {
            it.name.equals(name, ignoreCase = true) ||
            it.name.contains(name, ignoreCase = true) ||
            name.contains(it.name, ignoreCase = true)
        }?.avatarUrl?.let { url ->
            if (url.isNotBlank()) return url
        }

        // 2. Also search SharedPreferences
        try {
            val allPrefs = preferenceManager.prefs.all
            for ((key, value) in allPrefs) {
                if (key.startsWith("agent_") && value is String) {
                    val parts = value.split("|||")
                    if (parts.size >= 3) {
                        val cachedName = parts[0]
                        val cachedAvatarUrl = parts[2]
                        if (cachedName.equals(name, ignoreCase = true) ||
                            cachedName.contains(name, ignoreCase = true) ||
                            name.contains(cachedName, ignoreCase = true)) {
                            if (cachedAvatarUrl.isNotBlank()) {
                                return cachedAvatarUrl
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Safe fallback
        }
        return null
    }

    fun selectMonth(month: String) {
        selectedMonth.value = month
        fetchWeeklyMeetingIgListings(month)
        viewModelScope.launch {
            _syncStatus.value = SyncState.Loading
            
            // 2. Fetch all latest entries from Google Sheets for the selected month (No clearLocal to prevent empty-state flickering)
            val result = repository.syncFromGoogleSheets()
            result.onSuccess {
                _syncStatus.value = SyncState.Success("Berhasil dialihkan ke sheet $month!")
            }.onFailure { err ->
                _syncStatus.value = SyncState.Error("Gagal mengambil data $month: ${err.localizedMessage ?: "Masalah koneksi"}")
            }
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        isDarkTheme.value = enabled
        preferenceManager.isDarkTheme = enabled
    }

    fun setThemeStyle(style: String) {
        selectedThemeStyle.value = style
        preferenceManager.selectedThemeStyle = style
    }

    fun updateWeeklyMeetingSchedule(
        dateStr: String,
        row: Int,
        colIndex: Int,
        jadwalPosting: String,
        photoMonth: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        val baseUrl = appsScriptUrl.value
        if (baseUrl.isBlank()) {
            onResult(false, "URL Google Apps Script belum diatur di menu Setting.")
            return
        }

        // Deteksi bulan dari dateStr (format: "Selasa, 8 Juli 2026" atau "2026-07-08")
        val monthNames = listOf("Januari","Februari","Maret","April","Mei","Juni",
                                "Juli","Agustus","September","Oktober","November","Desember")
        val detectedMonth = when {
            dateStr.contains("-01-") || dateStr.contains("Januari") -> "Januari"
            dateStr.contains("-02-") || dateStr.contains("Februari") -> "Februari"
            dateStr.contains("-03-") || dateStr.contains("Maret") -> "Maret"
            dateStr.contains("-04-") || dateStr.contains("April") -> "April"
            dateStr.contains("-05-") || dateStr.contains("Mei") -> "Mei"
            dateStr.contains("-06-") || dateStr.contains("Juni") -> "Juni"
            dateStr.contains("-07-") || dateStr.contains("Juli") -> "Juli"
            dateStr.contains("-08-") || dateStr.contains("Agustus") -> "Agustus"
            dateStr.contains("-09-") || dateStr.contains("September") -> "September"
            dateStr.contains("-10-") || dateStr.contains("Oktober") -> "Oktober"
            dateStr.contains("-11-") || dateStr.contains("November") -> "November"
            dateStr.contains("-12-") || dateStr.contains("Desember") -> "Desember"
            else -> photoMonth.ifBlank { selectedMonth.value }
        }
        val sheetName = getWeeklyMeetingSheetNameForMonth(detectedMonth)

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val request = com.example.network.UpdateMeetingScheduleRequest(
                    sheetName = sheetName,
                    date = dateStr,
                    row = row,
                    colIndex = colIndex,
                    jadwalPosting = jadwalPosting
                )
                
                val response = apiService.updateMeetingSchedule(baseUrl, request)
                if (response.status.lowercase() == "success") {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(true, response.message)
                    }
                    // Refresh dengan bulan yang benar
                    fetchWeeklyMeetingIgListings(detectedMonth)
                    if (selectedMeetingDate.value != null && selectedMeetingMonth.value != null) {
                        fetchMeetingListings(selectedMeetingMonth.value!!, selectedMeetingDate.value!!)
                    }
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(false, response.message)
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "Gagal mengubah tanggal posting: ${e.localizedMessage ?: "Masalah koneksi"}")
                }
            }
        }
    }

    fun setFormatDone(format: String) {
        formatDone.value = format
        preferenceManager.formatDone = format
    }

    fun setFormatNotDone(format: String) {
        formatNotDone.value = format
        preferenceManager.formatNotDone = format
    }

    // Cache for scraped listing sold status: idListing -> Boolean
    private val _listingSoldMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val listingSoldMap: StateFlow<Map<String, Boolean>> = _listingSoldMap.asStateFlow()

    // Cache for scraper image URLs: idListing -> Image URL
    private val _listingImagesMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val listingImagesMap: StateFlow<Map<String, String>> = _listingImagesMap.asStateFlow()

    // Cache for scraper all image URLs (Gallery): idListing -> List of image URLs
    private val _listingImagesGalleryMap = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val listingImagesGalleryMap: StateFlow<Map<String, List<String>>> = _listingImagesGalleryMap.asStateFlow()

    // Cache for scraped Agent/ME Contact Info: idListing -> AgentInfo
    private val _agentInfoMap = MutableStateFlow<Map<String, AgentInfo>>(emptyMap())
    val agentInfoMap: StateFlow<Map<String, AgentInfo>> = _agentInfoMap.asStateFlow()

    // Cache for scraped listing price: idListing -> Price
    private val _listingPriceMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val listingPriceMap: StateFlow<Map<String, String>> = _listingPriceMap.asStateFlow()

    // Cache for scraped listing descriptions: idListing -> Description
    private val _listingDescMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val listingDescMap: StateFlow<Map<String, String>> = _listingDescMap.asStateFlow()

    // Cache for scraped listing titles: idListing -> Title
    private val _listingTitleMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val listingTitleMap: StateFlow<Map<String, String>> = _listingTitleMap.asStateFlow()

    // Track active scraping listing IDs
    private val _activeScrapes = MutableStateFlow<Set<String>>(emptySet())
    val activeScrapes: StateFlow<Set<String>> = _activeScrapes.asStateFlow()

    fun fetchListingImageIfNeeded(idListing: String, defaultMeName: String = "", forceRefresh: Boolean = false) {
        val cleanId = idListing.trim()
        if (cleanId.isBlank()) return
        
        if (!forceRefresh) {
            // 1. Check if already in-memory
            if (_listingImagesMap.value.containsKey(cleanId) && 
                _listingImagesGalleryMap.value.containsKey(cleanId) &&
                _listingTitleMap.value.containsKey(cleanId) &&
                _listingDescMap.value.containsKey(cleanId)) return

            // 2. Check if already in SharedPreferences (the persistent cache)
            val cachedImg = preferenceManager.getListingImage(cleanId)
            val cachedGallery = preferenceManager.getListingGallery(cleanId)
            val cachedTitle = preferenceManager.getListingTitle(cleanId)
            val cachedDesc = preferenceManager.getListingDesc(cleanId)
            val cachedPrice = preferenceManager.getListingPrice(cleanId)
            val cachedAgentStr = preferenceManager.getAgentInfo(cleanId)
            val cachedSold = preferenceManager.getListingSold(cleanId)

            if (cachedSold) {
                _listingSoldMap.update { it + (cleanId to true) }
            } else {
                _listingSoldMap.update { it + (cleanId to false) }
            }

            if (cachedImg != null && cachedGallery != null && cachedTitle != null && cachedDesc != null) {
                _listingImagesMap.update { it + (cleanId to cachedImg) }
                _listingImagesGalleryMap.update { it + (cleanId to cachedGallery) }
                _listingTitleMap.update { it + (cleanId to cachedTitle) }
                _listingDescMap.update { it + (cleanId to cachedDesc) }
                if (cachedPrice != null) {
                    _listingPriceMap.update { it + (cleanId to cachedPrice) }
                }
                if (cachedAgentStr != null) {
                    val parts = cachedAgentStr.split("|||")
                    if (parts.size >= 3) {
                        val aInfo = AgentInfo(
                            name = parts[0],
                            phone = getAgentPhoneByName(parts[0]),
                            waUrl = parts[1],
                            avatarUrl = parts[2],
                            email = getAgentEmailByName(parts[0]),
                            instagram = getAgentInstagramByName(parts[0])
                        )
                        _agentInfoMap.update { it + (cleanId to aInfo) }
                    }
                }
                return
            }
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _activeScrapes.update { it + cleanId }
            val url = "https://raywhitecipete.net/ListingView/Detail/$cleanId"
            try {
                // Create a trust-all trust manager to bypass SSL/TLS handshake issues on Cloud and Android
                val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
                    object : javax.net.ssl.X509TrustManager {
                        override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                        override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                    }
                )
                
                val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                val sslSocketFactory = sslContext.socketFactory
                
                val client = okhttp3.OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
                    .hostnameVerifier { _, _ -> true }
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()

                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9,id;q=0.8")
                    .build()
                client.newCall(request).execute().use { response ->
                    val finalUrl = response.request.url.toString()
                    val isRedirectedToHome = finalUrl == "https://raywhitecipete.net" || finalUrl == "https://raywhitecipete.net/" || finalUrl.startsWith("https://raywhitecipete.net/?")
                    
                    if (isRedirectedToHome || response.code == 500) {
                        _listingSoldMap.update { it + (cleanId to true) }
                        preferenceManager.saveListingSold(cleanId, true)
                        return@use
                    }

                    if (response.isSuccessful) {
                        val html = response.body?.string() ?: ""
                        if (html.contains("Server Error") || html.contains("Server Error in") || html.contains("Runtime Error")) {
                            _listingSoldMap.update { it + (cleanId to true) }
                            preferenceManager.saveListingSold(cleanId, true)
                            return@use
                        }

                        _listingSoldMap.update { it + (cleanId to false) }
                        preferenceManager.saveListingSold(cleanId, false)
                        
                        val doc = org.jsoup.Jsoup.parse(html)
                        
                        // Parse Images robustly using Jsoup
                        val candidateUrls = mutableListOf<String>()
                        doc.select("img").forEach { img ->
                            var src = img.attr("src")?.trim() ?: ""
                            val dataSrc = img.attr("data-src")?.trim() ?: ""
                            val dataOrig = img.attr("data-original")?.trim() ?: ""
                            if (src.isBlank() || src.contains("placeholder") || src.contains("blank")) {
                                if (dataSrc.isNotBlank()) src = dataSrc
                                else if (dataOrig.isNotBlank()) src = dataOrig
                            }
                            if (src.isNotBlank()) {
                                candidateUrls.add(src)
                            }
                        }
                        val allImages = mutableListOf<String>()
                        
                        // Pick out all valid property gallery/upload images
                        for (src in candidateUrls) {
                            val lower = src.lowercase()
                            val matchesAgentName = AGENT_CONTACT_LIST.any { contact ->
                                lower.contains("/" + contact.nameKey) || 
                                lower.contains(contact.nameKey + ".") || 
                                lower.contains(contact.nameKey + "-") || 
                                lower.contains(contact.nameKey + "_")
                            }
                            if (!lower.contains("logo") &&
                                !lower.contains("icon") &&
                                !lower.contains("avatar") &&
                                !lower.contains("marker") &&
                                !lower.contains("theme") &&
                                !lower.contains("banner") &&
                                !lower.contains("assets") &&
                                !lower.contains("raywhite") &&
                                !lower.contains("social") &&
                                !lower.contains("agent") &&
                                !lower.contains("profile") &&
                                !lower.contains("team") &&
                                !lower.contains("member") &&
                                !lower.contains("staff") &&
                                !lower.contains("/me/") &&
                                !matchesAgentName &&
                                !lower.endsWith(".svg") &&
                                (src.startsWith("http") || src.startsWith("/"))) {
                                
                                val fullUrl = if (src.startsWith("/")) {
                                    "https://raywhitecipete.net" + src
                                } else {
                                    src
                                }
                                if (!allImages.contains(fullUrl)) {
                                    allImages.add(fullUrl)
                                }
                            }
                        }
                        
                        // Extract Agent Personal Photo from HTML (using precise CSS selectors matching user's xpath, or falling back)
                        val agentImages = mutableListOf<String>()
                        val agentContainer = doc.select("body > section:nth-of-type(3) > div > div > div > div > div > div > div > div:nth-child(2) > div:nth-child(2)").firstOrNull() ?:
                                             doc.select("section:nth-of-type(3) .row > div:nth-child(2)").firstOrNull() ?:
                                             doc.select(".agent-section, .agent-info, .agent-detail").firstOrNull()
                        
                        if (agentContainer != null) {
                            val imgElements = agentContainer.select("img")
                            for (img in imgElements) {
                                var src = img.attr("src")?.trim() ?: ""
                                val dataSrc = img.attr("data-src")?.trim() ?: ""
                                val dataOrig = img.attr("data-original")?.trim() ?: ""
                                if (src.isBlank() || src.contains("placeholder") || src.contains("blank")) {
                                    if (dataSrc.isNotBlank()) src = dataSrc
                                    else if (dataOrig.isNotBlank()) src = dataOrig
                                }
                                if (src.isNotBlank() && !src.contains("logo") && !src.contains("icon")) {
                                    val fullUrl = if (src.startsWith("/")) {
                                        "https://raywhitecipete.net" + src
                                    } else {
                                        src
                                    }
                                    if (!agentImages.contains(fullUrl)) {
                                        agentImages.add(fullUrl)
                                    }
                                }
                            }
                        }
                        
                        // Heuristic fallback if precise selectors returned no images
                        if (agentImages.isEmpty()) {
                            for (src in candidateUrls) {
                                val lower = src.lowercase()
                                val matchesAgentName = AGENT_CONTACT_LIST.any { contact ->
                                    lower.contains("/" + contact.nameKey) || 
                                    lower.contains(contact.nameKey + ".") || 
                                    lower.contains(contact.nameKey + "-") || 
                                    lower.contains(contact.nameKey + "_")
                                }
                                if (lower.contains("agent") || lower.contains("avatar") || lower.contains("profile") || lower.contains("team") || lower.contains("member") || lower.contains("staff") || lower.contains("/me/") || matchesAgentName) {
                                    val fullUrl = if (src.startsWith("/")) {
                                        "https://raywhitecipete.net" + src
                                    } else {
                                        src
                                    }
                                    if (!agentImages.contains(fullUrl)) {
                                        agentImages.add(fullUrl)
                                    }
                                }
                            }
                        }
                        
                        val agentAvatarUrl = agentImages.joinToString(",")

                        // Parse WhatsApp / Agent Info
                        val waRegex = """(?:https?:)?//(?:api\.whatsapp\.com/send|wa\.me)/?[^\s"'>]*|whatsapp://[^\s"'>]*""".toRegex(RegexOption.IGNORE_CASE)
                        val waMatches = waRegex.findAll(html).map { it.value }.toList()
                        
                        var finalAgentName = defaultMeName.trim()
                        var finalAgentPhone = getAgentPhoneByName(defaultMeName)
                        var finalWaUrl = ""
                        
                        var bestAgentName = defaultMeName.trim()
                        var bestAgentPhone = ""
                        var bestWaUrl = ""
                        
                        for (waUrl in waMatches) {
                            val decoded = try { java.net.URLDecoder.decode(waUrl, "UTF-8") } catch(e: Exception) { waUrl }
                            
                            // Extract phone
                            var phone = ""
                            val phoneMatch = """phone=([+\d]+)""".toRegex().find(waUrl)
                            if (phoneMatch != null) {
                                phone = phoneMatch.groupValues[1]
                            } else {
                                val waMeMatch = """wa\.me/([+\d]+)""".toRegex().find(waUrl)
                                if (waMeMatch != null) {
                                    phone = waMeMatch.groupValues[1]
                                }
                            }
                            
                            if (phone.isBlank()) {
                                val numMatch = """08\d{8,11}""".toRegex().find(decoded)
                                if (numMatch != null) {
                                    phone = numMatch.value
                                }
                            }
                            
                            // Check if phone matches the default office admin digits
                            val digitsOnly = phone.replace("+", "").removePrefix("62").removePrefix("0")
                            val isOfficeAdminLine = digitsOnly == "85169671344"
                            
                            // Extract Name
                            var name = ""
                            val hubungiMatch = """Hubungi:[ \n\r]*([A-Za-z ]+)[, \n\r]""".toRegex(RegexOption.IGNORE_CASE).find(decoded)
                            if (hubungiMatch != null) {
                                name = hubungiMatch.groupValues[1].trim()
                            } else {
                                val hubungiNoColon = """Hubungi\s+([A-Za-z ]+)""".toRegex(RegexOption.IGNORE_CASE).find(decoded)
                                if (hubungiNoColon != null) {
                                    name = hubungiNoColon.groupValues[1].trim()
                                }
                            }
                            
                            if (phone.isNotBlank()) {
                                // Prioritize the personal agent number over the floating office support number
                                if (bestAgentPhone.isBlank() || (!isOfficeAdminLine && bestAgentPhone.replace("+", "").removePrefix("62").removePrefix("0") == "85169671344")) {
                                    bestAgentPhone = phone
                                    if (name.isNotBlank()) {
                                        bestAgentName = name
                                    }
                                    bestWaUrl = waUrl
                                    // If we found a personal number (non-office), we can stop
                                    if (!isOfficeAdminLine) {
                                        break
                                    }
                                }
                            }
                        }
                        
                        if (bestAgentPhone.isNotBlank()) {
                            finalAgentPhone = bestAgentPhone
                            finalAgentName = bestAgentName
                            finalWaUrl = bestWaUrl
                        }
                        
                        // Text heuristics search/fallback for phone and name if no explicit Wa link worked
                        if (finalAgentPhone.isBlank()) {
                            val phoneRegex = """(?:08|\+628|628)\d{1,4}[-.\s]?\d{3,4}[-.\s]?\d{3,4}""".toRegex()
                            val match = phoneRegex.find(html)
                            if (match != null) {
                                finalAgentPhone = match.groupValues[0].replace("[-\\s\\+]".toRegex(), "")
                                val startIdx = maxOf(0, match.range.first - 150)
                                val context = html.substring(startIdx, match.range.first)
                                val cleanContext = context.replace("<[^>]*>".toRegex(), " ")
                                val nameMatch = """Hubungi\s*:\s*([A-Za-z\s]+)""".toRegex(RegexOption.IGNORE_CASE).find(cleanContext)
                                if (nameMatch != null) {
                                    finalAgentName = nameMatch.groupValues[1].trim()
                                } else {
                                    val words = cleanContext.trim().split("\\s+".toRegex())
                                    if (words.isNotEmpty()) {
                                        val potentialName = words.takeLast(4).filter { it.firstOrNull()?.isUpperCase() == true && it.length > 2 }.joinToString(" ")
                                        if (potentialName.isNotBlank() && !potentialName.contains("Share", ignoreCase = true) && !potentialName.contains("Rp", ignoreCase = true)) {
                                            finalAgentName = potentialName
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Parse agent name using Jsoup selectors
                        var agentNameFromDoc = ""
                        try {
                            val doc = org.jsoup.Jsoup.parse(html)
                            
                            // 1. Check for co-listing using precise selectors
                            val co1 = doc.select("body > section:nth-of-type(3) > div > div > div > div > div > div > div > div:nth-child(2) > div:nth-child(2) > div > div:nth-child(1) > div > div:nth-child(1) > h5").firstOrNull()?.text()?.trim() ?:
                                      doc.select("body > section:nth-of-type(3) > div > div > div > div > div > div > div > div:nth-child(2) > div:nth-child(2) > div > div:nth-child(1) h5").firstOrNull()?.text()?.trim() ?:
                                      doc.select("body > section:nth-of-type(3) > div > div > div > div > div > div > div > div:nth-child(2) > div:nth-child(2) > div > div:nth-child(1) h3").firstOrNull()?.text()?.trim() ?:
                                      doc.select("body > section:nth-of-type(3) > div > div > div > div > div > div > div > div:nth-child(2) > div:nth-child(2) > div > div:nth-child(1) h4").firstOrNull()?.text()?.trim()

                            val co2 = doc.select("body > section:nth-of-type(3) > div > div > div > div > div > div > div > div:nth-child(2) > div:nth-child(2) > div > div:nth-child(2) > div > div:nth-child(1) > div > h3").firstOrNull()?.text()?.trim() ?:
                                      doc.select("body > section:nth-of-type(3) > div > div > div > div > div > div > div > div:nth-child(2) > div:nth-child(2) > div > div:nth-child(2) h3").firstOrNull()?.text()?.trim() ?:
                                      doc.select("body > section:nth-of-type(3) > div > div > div > div > div > div > div > div:nth-child(2) > div:nth-child(2) > div > div:nth-child(2) h5").firstOrNull()?.text()?.trim() ?:
                                      doc.select("body > section:nth-of-type(3) > div > div > div > div > div > div > div > div:nth-child(2) > div:nth-child(2) > div > div:nth-child(2) h4").firstOrNull()?.text()?.trim()

                            if (!co1.isNullOrBlank() && !co2.isNullOrBlank() && isValidAgentName(co1) && isValidAgentName(co2)) {
                                agentNameFromDoc = formatAgentName("$co1 / $co2")
                            } else {
                                // 2. If co1 or co2 wasn't detected/valid, let's scan all agent blocks in section[3]/div/.../div[2]/div[2]
                                val agentContainer = doc.select("body > section:nth-of-type(3) > div > div > div > div > div > div > div > div:nth-child(2) > div:nth-child(2)").firstOrNull() ?:
                                                     doc.select("section:nth-of-type(3) .row > div:nth-child(2)").firstOrNull() ?:
                                                     doc.select(".agent-section, .agent-info, .agent-detail").firstOrNull()
                                
                                val parsedNames = mutableListOf<String>()
                                if (agentContainer != null) {
                                    val agentBlocks = agentContainer.select("div > div")
                                    if (agentBlocks.isNotEmpty()) {
                                        for (block in agentBlocks) {
                                            val headings = block.select("h3, h4, h5, .agent-name, .name, div[class*=name], div[id*=name]")
                                            for (h in headings) {
                                                val txt = h.text().trim()
                                                if (isValidAgentName(txt)) {
                                                    parsedNames.add(txt)
                                                    break
                                                }
                                            }
                                        }
                                    }
                                    
                                    // If no separate blocks, check any heading inside agentContainer
                                    if (parsedNames.isEmpty()) {
                                        val headings = agentContainer.select("h3, h4, h5, .agent-name, .name")
                                        for (h in headings) {
                                            val txt = h.text().trim()
                                            if (isValidAgentName(txt)) {
                                                parsedNames.add(txt)
                                            }
                                        }
                                    }
                                }
                                
                                if (parsedNames.isNotEmpty()) {
                                    agentNameFromDoc = formatAgentName(parsedNames.distinct().joinToString(" / "))
                                } else {
                                    // 3. Fallback to single agent direct XPath
                                    val singleEl = doc.select("body > section:nth-of-type(3) > div > div > div > div > div > div > div > div:nth-child(2) > div:nth-child(2) > div > div > div > div:nth-child(1) > div").firstOrNull()
                                    val singleText = singleEl?.text()?.trim() ?: ""
                                    if (isValidAgentName(singleText)) {
                                        agentNameFromDoc = formatAgentName(singleText)
                                    }
                                }
                            }

                            // 4. Highly robust scan: search agentContainer/body text for names in AGENT_CONTACT_LIST
                            if (agentNameFromDoc.isBlank()) {
                                val agentContainer = doc.select("body > section:nth-of-type(3)").firstOrNull() ?: doc.body()
                                if (agentContainer != null) {
                                    val containerText = agentContainer.text().lowercase()
                                    val foundNames = mutableListOf<String>()
                                    for (contact in AGENT_CONTACT_LIST) {
                                        val key = contact.nameKey.lowercase()
                                        val wordRegex = "\\b${Regex.escape(key)}\\b".toRegex()
                                        if (containerText.contains(wordRegex)) {
                                            foundNames.add(contact.nameKey)
                                        }
                                    }
                                    if (foundNames.isNotEmpty()) {
                                        agentNameFromDoc = formatAgentName(foundNames.distinct().joinToString(" / "))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        if (agentNameFromDoc.isNotBlank()) {
                            finalAgentName = agentNameFromDoc
                        }

                        // Look up agent name from phone number if we have a phone but name is still blank or office admin
                        if (finalAgentPhone.isNotBlank() && (finalAgentName.isBlank() || finalAgentName.lowercase() == "ray white" || finalAgentName.lowercase() == "admin")) {
                            val lookupName = getAgentNameByPhone(finalAgentPhone)
                            if (lookupName.isNotBlank()) {
                                finalAgentName = formatAgentName(lookupName)
                            }
                        }

                        // If phone number is still empty, let's fall back to our local directory from name
                        if (finalAgentPhone.isBlank() && finalAgentName.isNotBlank()) {
                            finalAgentPhone = getAgentPhoneByName(finalAgentName)
                        }
                        
                        // Parse Price, Title & Description
                        var parsedPrice = ""
                        val priceCandidates = mutableListOf<String>()
                        try {
                            val doc = org.jsoup.Jsoup.parse(html)
                            // Extract h4 tags under section 3 first (as used on raywhitecipete details pages)
                            val h4s = doc.select("body > section:nth-of-type(3) h4, body > section:nth-of-type(3) h3, #buy h4, h4")
                            for (h4 in h4s) {
                                val txt = h4.text().trim()
                                if (txt.contains("Rp", ignoreCase = true)) {
                                    if (!priceCandidates.any { it.equals(txt, ignoreCase = true) }) {
                                        priceCandidates.add(txt)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        if (priceCandidates.isEmpty()) {
                            val priceRegex = """Rp\.?\s*([0-9\.,]+(?:\s*(?:Milyar|M|Juta|J|Tahun|Thn|Bulan|Bln))?)""".toRegex(RegexOption.IGNORE_CASE)
                            val priceMatches = priceRegex.findAll(html)
                            for (m in priceMatches) {
                                val candidate = m.value.trim()
                                if (candidate.length > 5 && candidate.length < 40) {
                                    if (!priceCandidates.any { it.equals(candidate, ignoreCase = true) }) {
                                        priceCandidates.add(candidate)
                                    }
                                }
                            }
                        }

                        parsedPrice = if (priceCandidates.isNotEmpty()) {
                            priceCandidates.joinToString(" / ")
                        } else {
                            "Hubungi Agent"
                        }

                        var parsedTitle = ""
                        var parsedDesc = ""
                        try {
                            val doc = org.jsoup.Jsoup.parse(html)
                            
                            // 1. Scrape Title
                            var titleEl = doc.select("body > section:nth-of-type(3) > div > div > div > div > div > div > div > div:nth-child(2) > div:nth-child(1) > div > h4").firstOrNull()
                            if (titleEl == null) {
                                titleEl = doc.select("body > section:nth-of-type(3) h4").firstOrNull()
                            }
                            if (titleEl == null) {
                                titleEl = doc.select("#buy h4").firstOrNull()
                            }
                            if (titleEl == null) {
                                titleEl = doc.select("#listingName").firstOrNull()
                            }
                            if (titleEl == null) {
                                titleEl = doc.select("h4").firstOrNull()
                            }
                            if (titleEl != null) {
                                parsedTitle = titleEl.text().trim()
                            }
                            
                            // 2. Scrape Description based on user's exact xpath locations
                            // We translate user's xpath to Jsoup selectors:
                            // Target parent div: "#buy > div > div > div > div > div > div > div > div:nth-child(2) > div:nth-child(1) > div"
                            var targetDiv: org.jsoup.nodes.Element? = doc.select("#buy > div > div > div > div > div > div > div > div:nth-child(2) > div:nth-child(1) > div").firstOrNull()
                            
                            if (targetDiv == null) {
                                // Fallback: any div inside #buy that has h5 or p
                                val buyEl = doc.getElementById("buy")
                                if (buyEl != null) {
                                    val h5 = buyEl.select("h5").firstOrNull { it.text().contains("Deskripsi", ignoreCase = true) }
                                    if (h5 != null) {
                                        targetDiv = h5.parent()
                                    } else {
                                        val firstP = buyEl.select("p").firstOrNull()
                                        if (firstP != null) {
                                            targetDiv = firstP.parent()
                                        }
                                    }
                                }
                            }
                            
                            if (targetDiv != null) {
                                val sb = java.lang.StringBuilder()
                                
                                // 1. Spek text (second p element inside targetDiv)
                                val pList = targetDiv.select("p")
                                if (pList.size >= 2) {
                                    val p2 = pList[1]
                                    val spekText = getCleanTextWithNewlines(p2).trim()
                                    if (spekText.isNotBlank()) {
                                        sb.append(spekText)
                                        sb.append("\n\n")
                                    }
                                } else if (pList.isNotEmpty()) {
                                    // Fallback to any p containing "spek" or similar, or first p if only one
                                    val spekP = pList.firstOrNull { it.text().lowercase().contains("spek") } ?: pList[0]
                                    val spekText = getCleanTextWithNewlines(spekP).trim()
                                    if (spekText.isNotBlank()) {
                                        sb.append(spekText)
                                        sb.append("\n\n")
                                    }
                                }
                                
                                // 2. Deskripsi Lengkap header and text below it
                                val h5 = targetDiv.select("h5").firstOrNull()
                                if (h5 != null) {
                                    val h5Text = getCleanTextWithNewlines(h5).trim()
                                    if (h5Text.isNotBlank()) {
                                        sb.append(h5Text)
                                        sb.append("\n\n")
                                    }
                                    
                                    // Append all nodes below h5
                                    var nextNode = h5.nextSibling()
                                    while (nextNode != null) {
                                        sb.append(getCleanTextWithNewlines(nextNode))
                                        nextNode = nextNode.nextSibling()
                                    }
                                } else {
                                    // Fallback if h5 doesn't exist: append all remaining text nodes and tags from targetDiv that aren't the title or first/second p
                                    val allChildren = targetDiv.childNodes()
                                    var skipPCount = 0
                                    for (child in allChildren) {
                                        if (child is org.jsoup.nodes.Element) {
                                            val tagName = child.tagName()
                                            if (tagName == "h4") continue // Skip title
                                            if (tagName == "p") {
                                                skipPCount++
                                                if (skipPCount <= 2) continue // Skip p[1] and p[2] (p[2] was appended already)
                                            }
                                        }
                                        sb.append(getCleanTextWithNewlines(child))
                                    }
                                }
                                parsedDesc = sb.toString().trim()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // Robust fallback for description if Jsoup did not find it
                        if (parsedDesc.isBlank()) {
                            val descDivRegex = """<div[^>]+(?:class|id)=["']([^"']*(?:desc|content|detail)[^"']*)["'][^>]*>([\s\S]*?)</div>""".toRegex(RegexOption.IGNORE_CASE)
                            val descMatches = descDivRegex.findAll(html)
                            var longestText = ""
                            for (match in descMatches) {
                                val classOrId = match.groupValues[1].lowercase()
                                if (classOrId.contains("header") || classOrId.contains("nav") || classOrId.contains("footer") || classOrId.contains("menu")) continue
                                val innerHtml = match.groupValues[2]
                                val cleanTxt = innerHtml.replace("<[^>]*>".toRegex(), " ")
                                                        .replace("&nbsp;".toRegex(), " ")
                                                        .replace("""\s+""".toRegex(), " ")
                                                        .trim()
                                if (cleanTxt.length > longestText.length && cleanTxt.length < 5000) {
                                    if (cleanTxt.lowercase().contains("luas") || cleanTxt.lowercase().contains("kamar") || cleanTxt.lowercase().contains("tanah") || cleanTxt.lowercase().contains("mandi") || cleanTxt.lowercase().contains("house")) {
                                        longestText = cleanTxt
                                    } else if (longestText.isEmpty() || !longestText.lowercase().contains("luas")) {
                                        longestText = cleanTxt
                                    }
                                }
                            }
                            if (longestText.length > 30) {
                                parsedDesc = longestText
                            } else {
                                val pRegex = """<p[^>]*>([\s\S]*?)</p>""".toRegex(RegexOption.IGNORE_CASE)
                                val pMatches = pRegex.findAll(html)
                                val pList = mutableListOf<String>()
                                for (pm in pMatches) {
                                    val txt = pm.groupValues[1].replace("<[^>]*>".toRegex(), " ").replace("&nbsp;".toRegex(), " ").trim()
                                    if (txt.length > 20 && !txt.contains("Copyright", ignoreCase = true) && !txt.contains("Ray White", ignoreCase = true)) {
                                        pList.add(txt)
                                    }
                                }
                                if (pList.isNotEmpty()) {
                                    parsedDesc = pList.joinToString("\n\n")
                                }
                            }
                        }
                        if (parsedDesc.isBlank()) {
                            parsedDesc = "Deskripsi tidak tersedia di websiteListing. Silakan hubungi agent terkait."
                        }

                        _listingPriceMap.update { current ->
                            current + (cleanId to parsedPrice)
                        }
                        _listingDescMap.update { current ->
                            current + (cleanId to parsedDesc)
                        }
                        if (parsedTitle.isNotBlank()) {
                            _listingTitleMap.update { current ->
                                current + (cleanId to parsedTitle)
                            }
                        }

                        // Update cache with results
                        if (allImages.isNotEmpty()) {
                            _listingImagesMap.update { current ->
                                current + (cleanId to allImages.first())
                            }
                            _listingImagesGalleryMap.update { current ->
                                current + (cleanId to allImages)
                            }
                        }
                        
                        val agentInfo = AgentInfo(
                            name = finalAgentName,
                            phone = getAgentPhoneByName(finalAgentName),
                            waUrl = finalWaUrl,
                            avatarUrl = agentAvatarUrl,
                            email = getAgentEmailByName(finalAgentName),
                            instagram = getAgentInstagramByName(finalAgentName)
                        )
                        _agentInfoMap.update { current ->
                            current + (cleanId to agentInfo)
                        }

                        // Save to persistent cache so it never needs to be scraped over network again
                        preferenceManager.saveListingImage(cleanId, allImages.firstOrNull() ?: "")
                        preferenceManager.saveListingGallery(cleanId, allImages)
                        preferenceManager.saveListingTitle(cleanId, parsedTitle)
                        preferenceManager.saveListingDesc(cleanId, parsedDesc)
                        preferenceManager.saveListingPrice(cleanId, parsedPrice)
                        preferenceManager.saveAgentInfo(cleanId, finalAgentName, finalWaUrl, agentAvatarUrl)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _activeScrapes.update { it - cleanId }
            }
        }
    }

    // Status overlays
    private val _syncStatus = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncStatus: StateFlow<SyncState> = _syncStatus.asStateFlow()

    private val _submitStatus = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitStatus: StateFlow<SubmitState> = _submitStatus.asStateFlow()

    data class AppUpdate(
        val version: String = "",
        val downloadUrl: String = "",
        val changelog: String = ""
    )

    private val _appUpdateState = MutableStateFlow<AppUpdate?>(null)
    val appUpdateState: StateFlow<AppUpdate?> = _appUpdateState.asStateFlow()

    private fun isDateInSelectedMonth(dateStr: String, selectedMonth: String): Boolean {
        if (dateStr.length < 7) return false
        val yearPart = dateStr.substring(0, 4) // "2026"
        val monthPart = dateStr.substring(5, 7) // "06"
        
        val monthName = when (monthPart) {
            "01" -> "Januari"
            "02" -> "Februari"
            "03" -> "Maret"
            "04" -> "April"
            "05" -> "Mei"
            "06" -> "Juni"
            "07" -> "Juli"
            "08" -> "Agustus"
            "09" -> "September"
            "10" -> "Oktober"
            "11" -> "November"
            "12" -> "Desember"
            else -> ""
        }
        val targetMonthYear = "$monthName $yearPart".lowercase().trim()
        return targetMonthYear == selectedMonth.lowercase().trim()
    }

    data class FilterCriteria(
        val query: String,
        val staff: String,
        val start: String?,
        val end: String?
    )

    private val filterCriteria: Flow<FilterCriteria> = combine(
        searchQuery,
        filterStaff,
        filterStartDate,
        filterEndDate
    ) { query, staff, start, end ->
        FilterCriteria(query, staff, start, end)
    }

    // Database entries
    val allSchedules: StateFlow<List<Schedule>> = repository.allSchedules
        .map { list -> list.filter { it.idListing.trim() != "VERSION_CHECK" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered schedules for dashboard using combined Flows
    val filteredSchedules: StateFlow<List<Schedule>> = combine(
        repository.allSchedules,
        selectedMonth,
        filterCriteria
    ) { list, activeMonth, criteria ->
        list.filter { item ->
            // Filter by active month in dashboard list
            val inSelectedMonth = if (item.sheetName.isNotBlank()) {
                item.sheetName.equals(activeMonth, ignoreCase = true)
            } else {
                isDateInSelectedMonth(item.tanggal, activeMonth)
            }
            if (!inSelectedMonth) return@filter false

            val matchesQuery = criteria.query.isBlank() ||
                    item.namaMe.contains(criteria.query, ignoreCase = true) ||
                    item.idListing.contains(criteria.query, ignoreCase = true) ||
                    item.lokasi.contains(criteria.query, ignoreCase = true)
            
            val matchesStaff = criteria.staff == "Semua" || criteria.staff.isBlank() || item.staff.equals(criteria.staff, ignoreCase = true)
            
            // Dates are stored as "yyyy-MM-dd"
            val matchesDate = (criteria.start.isNullOrBlank() || item.tanggal >= criteria.start) &&
                    (criteria.end.isNullOrBlank() || item.tanggal <= criteria.end)
            
            matchesQuery && matchesStaff && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List of all distinct staffs for filtering dropdown
    val staffList: StateFlow<List<String>> = repository.allSchedules
        .map { list ->
            val listStaffs = list.map { it.staff }.filter { it.isNotBlank() }.distinct().sorted()
            listOf("Semua") + listStaffs
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Semua"))

    // Preset staff choices for form autocompletion
    val defaultStaffPresets = listOf(
        "Doni Kusuma",
        "Randi Pratama",
        "Anita Wijaya",
        "Yusuf Hakim",
        "Rina Selvia",
        "Eko Prasetyo"
    )

    init {
        // Seed and load custom agent contacts from Room database
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val dao = db.agentContactDao()
                val existing = dao.getAllAgentContacts()
                if (existing.isEmpty()) {
                    val initialList = AGENT_CONTACT_LIST.map { ac ->
                        AgentContactEntity(
                            nameKey = ac.nameKey,
                            displayName = capitalizeName(ac.nameKey),
                            phone = ac.phone,
                            email = ac.email,
                            instagram = ac.instagram,
                            avatarUrl = ac.avatarUrl
                        )
                    }
                    dao.insertAllAgentContacts(initialList)
                } else {
                    // Update blank avatarUrls for existing contacts if they have a preset avatar
                    val toUpdate = existing.filter { it.avatarUrl.isBlank() }.mapNotNull { ent ->
                        val preset = AGENT_CONTACT_LIST.find { it.nameKey == ent.nameKey }
                        if (preset != null && preset.avatarUrl.isNotBlank()) {
                            ent.copy(avatarUrl = preset.avatarUrl)
                        } else {
                            null
                        }
                    }
                    if (toUpdate.isNotEmpty()) {
                        dao.insertAllAgentContacts(toUpdate)
                    }
                }
                
                // Collect and synchronize to the global cache
                dao.getAllAgentContactsFlow().collect { list ->
                    cachedAgentContactsList = list
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Reset lastAutofilledId when formListingId is changed or cleared
        viewModelScope.launch {
            formListingId.map { it.trim() }.distinctUntilChanged().collect { id ->
                if (id != lastAutofilledId) {
                    lastAutofilledId = null
                }
                if (id.isBlank()) {
                    formNamaMe.value = ""
                    formLokasi.value = ""
                }
            }
        }

        // Real-time autofill reactive flow
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            formListingId
                .debounce(800)
                .map { it.trim() }
                .distinctUntilChanged()
                .collect { id ->
                    if (id.isNotBlank() && id.length >= 3) {
                        fetchListingImageIfNeeded(id)
                    }
                }
        }

        viewModelScope.launch {
            combine(
                formListingId.map { it.trim() }.distinctUntilChanged(),
                agentInfoMap,
                listingTitleMap,
                listingDescMap
            ) { currentId, agents, titles, descs ->
                if (currentId.isNotBlank() && currentId.length >= 3) {
                    val normalizedCurrentId = normalizeIdListing(currentId)
                    // Pull from the local Room DB (Google Sheets cached values)
                    val schedules = allSchedules.value
                    val tasks = allEditFotoTasks.value

                    // 1. Try Google Sheet schedules first
                    val existingSchedule = schedules.find { 
                        normalizeIdListing(it.idListing).equals(normalizedCurrentId, ignoreCase = true) && 
                        it.namaMe.isNotBlank() 
                    }
                    // 2. Try Google Sheet edit foto tasks
                    val existingTask = tasks.find { 
                        normalizeIdListing(it.idListing).equals(normalizedCurrentId, ignoreCase = true) && 
                        it.namaMe.isNotBlank() 
                    }

                    val sheetMe = existingSchedule?.namaMe?.trim() ?: existingTask?.namaMe?.trim()
                    // Get location from sheet schedule or parse from task title/notes
                    val sheetLocation = existingSchedule?.lokasi?.trim() ?: existingTask?.judul?.let { parseLocationFromScraped(it, "") } ?: ""

                    val scrapedAgent = agents[currentId] ?: agents[normalizedCurrentId]
                    val title = titles[currentId] ?: titles[normalizedCurrentId]
                    val desc = descs[currentId] ?: descs[normalizedCurrentId]

                    // Prioritize sheet ME name if available, otherwise scraped agent name
                    val finalMe = if (!sheetMe.isNullOrBlank()) sheetMe else scrapedAgent?.name ?: ""
                    // Prioritize sheet location if available, otherwise scraped location
                    val finalLoc = if (sheetLocation.isNotBlank()) {
                        sheetLocation
                    } else if (!title.isNullOrBlank() || !desc.isNullOrBlank()) {
                        parseLocationFromScraped(title ?: "", desc ?: "")
                    } else {
                        ""
                    }

                    if (finalMe.isNotBlank() || finalLoc.isNotBlank()) {
                        val dummyAgent = AgentInfo(
                            name = finalMe,
                            phone = "",
                            waUrl = "",
                            avatarUrl = "",
                            email = "",
                            instagram = ""
                        )
                        AutofillData(currentId, dummyAgent, finalLoc, null)
                    } else {
                        AutofillData(currentId, null, "", null)
                    }
                } else {
                    AutofillData(currentId, null, "", null)
                }
            }
            .collect { data ->
                val currentId = data.currentId
                val agent = data.agent
                val loc = data.title
                
                if (currentId == formListingId.value.trim()) {
                    if (agent != null && agent.name.isNotBlank()) {
                        formNamaMe.value = agent.name
                        formLokasi.value = loc ?: ""
                        lastAutofilledId = currentId
                    } else {
                        // Clear the stale autofilled fields if the new ID has no match and we had previously autofilled
                        if (lastAutofilledId != null) {
                            formNamaMe.value = ""
                            formLokasi.value = ""
                            lastAutofilledId = null
                        }
                    }
                }
            }
        }

        // Initialize with seeds and configure form defaults
        viewModelScope.launch {
            repository.seedMockDataIfEmpty()
            if (preferenceManager.appsScriptUrl.isNotBlank()) {
                syncData()
                fetchWeeklyMeetingIgListings(selectedMonth.value)
                fetchYearlyIgPostingHistory()
            }
            // Real-time background sync loop every 10 seconds
            while (true) {
                kotlinx.coroutines.delay(10000)
                if (preferenceManager.appsScriptUrl.isNotBlank()) {
                    if (_syncStatus.value !is SyncState.Loading) {
                        syncDataSilently()
                    }
                    selectedMeetingMonth.value?.let { month ->
                        selectedMeetingDate.value?.let { dateStr ->
                            fetchMeetingListingsSilently(month, dateStr)
                        }
                    }
                    selectedAbsenMonthIndex.value?.let { monthIdx ->
                        fetchAbsensiMeetingSilently(monthIdx)
                    }
                }
            }
        }
        
        // Trigger GitHub repository update checker on app launch
        checkForUpdates()

        // Auto-reactive Alarms scheduler based on live allSchedules state
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            allSchedules
                .debounce(1000)
                .collect { list ->
                    try {
                        com.example.receiver.AlarmReceiver.syncAllAlarms(application, list)
                    } catch (e: Exception) {
                        android.util.Log.e("ScheduleViewModel", "Failed to sync alarms: ${e.message}", e)
                    }
                }
        }

        setupChatNotificationsAndBadges()
        resetForm()
    }

    fun checkForUpdates() {
        com.example.util.AppUpdateChecker.checkForUpdate(
            context = getApplication(),
            onNewVersionFound = { info ->
                _appUpdateState.value = AppUpdate(
                    version = info.versionName,
                    downloadUrl = info.downloadUrl,
                    changelog = info.releaseNotes
                )
            },
            onNoUpdate = {
                _appUpdateState.value = null
            }
        )
    }

    fun isNewerVersion(server: String, current: String): Boolean {
        return try {
            val serverParts = server.trim().lowercase().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = current.trim().lowercase().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
            for (i in 0 until maxOf(serverParts.size, currentParts.size)) {
                val serverVal = serverParts.getOrElse(i) { 0 }
                val currentVal = currentParts.getOrElse(i) { 0 }
                if (serverVal > currentVal) return true
                if (serverVal < currentVal) return false
            }
            false
        } catch (e: Exception) {
            server != current && server > current
        }
    }

    fun normalizeIdListing(id: String): String {
        val trimmed = id.trim()
        if (trimmed.endsWith(".0")) {
            return trimmed.substring(0, trimmed.length - 2)
        }
        val doubleVal = trimmed.toDoubleOrNull()
        if (doubleVal != null) {
            if (doubleVal == doubleVal.toLong().toDouble()) {
                return doubleVal.toLong().toString()
            }
        }
        return trimmed
    }

    fun parseLocationFromScraped(title: String, desc: String): String {
        if (title.isBlank() && desc.isBlank()) return ""
        val titleLower = title.lowercase()
        val descLower = desc.lowercase()
        
        // Comprehensive list of Jakarta/Jabodetabek locations & areas
        val locations = listOf(
            // South Jakarta (Jakarta Selatan)
            "kebagusan", "cilandak", "cipete", "kemang", "jagakarsa", "pondok indah", "ampera", "kebayoran baru", "kebayoran lama", "kebayoran", "senopati", "bintaro", "tebet", "pejaten", "cilodong", "pasar minggu", "gandaria", "mampang prapatan", "mampang", "pancoran", "setiabudi", "kalibata", "ciganjur", "lenteng agung", "ragunan", "tanjung barat", "pesanggrahan", "cipulir", "pondok pinang", "lebak bulus", "fatmawati", "blok m", "radio dalam", "dharmawangsa", "darmawangsa", "panglima polim", "permata hijau", "senayan", "sudirman", "kuningan", "menteng", "prapanca", "wijaya", "cipete dalam", "cipete utara", "cipete selatan", "gandaria utara", "gandaria selatan", "pondok labu", "petukangan", "ulujami", "kebon baru", "manggarai", "pasar manggis", "karet semanggi", "karet pedurenan", "karet tengsin", "karet", "gatot subroto", "gatsu", "rasuna said", "mega kuningan", "scbd", "tebet barat", "tebet timur", "menteng dalam", "pengadegan", "pejaten barat", "pejaten timur", "jatipadang", "buncit", "warung buncit", "duren tiga", "bangka", "tendean", "kapten tendean", "petogogan", "melawai", "pulo", "cipulo", "kebayoran lama utara", "kebayoran lama selatan", "cilandak barat", "cilandak timur", "tanah kusir",
            // Depok & Bogor
            "cinere", "depok", "sawangan", "margonda", "cimanggis", "limo", "beji", "pancoran mas", "sentul", "bogor", "cibubur", "bedahan", "beji timur", "gandul", "pangkalan jati", "krukut", "meruyung", "grogol", "mampang depok", "depok jaya", "sukmajaya", "tapos", "harjamukti", "bojonggede", "citayam", "sentul city", "tanah sareal", "bogor utara", "bogor selatan", "bogor timur", "bogor barat",
            // Tangerang / South Tangerang (Tangerang Selatan)
            "bsd city", "bsd", "serpong", "alam sutera", "gading serpong", "karawaci", "ciputat", "pamulang", "bintaro jaya", "ciledug", "tangerang", "serpong utara", "bintaro sektor 1", "bintaro sektor 2", "bintaro sektor 3", "bintaro sektor 4", "bintaro sektor 5", "bintaro sektor 6", "bintaro sektor 7", "bintaro sektor 8", "bintaro sektor 9", "graha raya", "pondok cabe", "cirendeu", "rempoa", "jombang", "sawah baru", "serua", "setu", "cisauk", "pagedangan", "legok", "curug", "cikokol", "tangerang kota", "larangan", "pondok aren",
            // West Jakarta (Jakarta Barat)
            "puri indah", "kembangan", "kebon jeruk", "meruya", "tanjung duren", "tomang", "grogol", "slipi", "palmerah", "kalideres", "cengkareng", "meruya utara", "meruya selatan", "kembangan utara", "kembangan selatan", "permata buana", "taman aries", "intercon", "semesta", "kemanggisan", "jelambar", "kapuk",
            // East Jakarta (Jakarta Timur)
            "rawamangun", "duren sawit", "pulomas", "ciracas", "kramat jati", "makasar", "matraman", "pasar rebo", "cakung", "cipayung", "jatinegara", "kayu putih", "pondok kelapa", "pondok bambu", "klender", "condet", "halim", "cililitan",
            // Central Jakarta (Jakarta Pusat)
            "salemba", "tanah abang", "gambir", "kemayoran", "cempaka putih", "sawah besar", "cikini", "gondangdia", "senen", "benhil", "bendungan hilir", "petamburan",
            // North Jakarta (Jakarta Utara)
            "pantai indah kapuk", "pik", "kelapa gading", "pluit", "sunter", "ancol", "cilincing", "koja", "pademangan", "penjaringan", "pik 2", "muara karang",
            // Bekasi
            "jatiasih", "tambun", "cikarang", "harapan indah", "summarecon bekasi", "bekasi", "grand wisata", "galaxy", "taman galaxy", "kemang pratama", "jatibening", "pondok gede"
        )
        
        val sortedLocations = locations.sortedByDescending { it.length }
        
        // First, check for multi-word location matches in title to be more specific
        // For example, if both "cinere" and "depok" are in the title, we want to extract "Cinere Depok"!
        for (loc in sortedLocations) {
            if (titleLower.contains(loc)) {
                val capLoc = loc.split(" ").joinToString(" ") { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale("id", "ID")) else c.toString() } }
                
                // Let's check if the title contains another location right after/before it (like "Cinere Depok" or "Jagakarsa Jakarta Selatan")
                for (otherLoc in sortedLocations) {
                    if (otherLoc != loc && titleLower.contains("$loc $otherLoc")) {
                        val capOther = otherLoc.split(" ").joinToString(" ") { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale("id", "ID")) else c.toString() } }
                        return "$capLoc $capOther"
                    }
                    if (otherLoc != loc && titleLower.contains("$otherLoc $loc")) {
                        val capOther = otherLoc.split(" ").joinToString(" ") { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale("id", "ID")) else c.toString() } }
                        return "$capOther $capLoc"
                    }
                }
                return capLoc
            }
        }
        
        // Second, check desc for any location matches if title didn't match
        for (loc in sortedLocations) {
            if (descLower.contains(loc)) {
                return loc.split(" ").joinToString(" ") { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale("id", "ID")) else c.toString() } }
            }
        }
        
        // Last fallback: try to parse the region from phrases like "di daerah [X]" or "di [X]" in title
        val diPattern = """di\s+daerah\s+([A-Za-z]+)""".toRegex(RegexOption.IGNORE_CASE)
        val diMatch = diPattern.find(title)
        if (diMatch != null) {
            return diMatch.groupValues[1].replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("id", "ID")) else it.toString() }
        }
        
        val diOnlyPattern = """di\s+([A-Za-z]+)""".toRegex(RegexOption.IGNORE_CASE)
        val diOnlyMatch = diOnlyPattern.find(title)
        if (diOnlyMatch != null) {
            val candidate = diOnlyMatch.groupValues[1]
            if (candidate.lowercase() != "jual" && candidate.lowercase() != "sewa" && candidate.lowercase() != "atas") {
                return candidate.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("id", "ID")) else it.toString() }
            }
        }

        if (title.isNotBlank() && title != "(Tidak ada judul listing)") {
            val words = title.trim().split("\\s+".toRegex())
            if (words.size <= 3) {
                return title.trim()
            }
        }
        
        return ""
    }

    fun updateScheduleDirectly(schedule: Schedule, original: Schedule) {
        viewModelScope.launch {
            _syncStatus.value = SyncState.Loading
            val result = repository.updateSchedule(schedule, original)
            result.onSuccess {
                _syncStatus.value = SyncState.Success("Jadwal Berhasil Diperbarui!")
                syncDataSilently()
            }.onFailure { err ->
                _syncStatus.value = SyncState.Error("Simpan Lokal Sukses (Sheets tertunda: ${err.message})")
                syncDataSilently()
            }
        }
    }

    fun resetForm() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()

        formListingId.value = ""
        formNamaMe.value = ""
        formLokasi.value = ""
        formTanggal.value = dateFormat.format(now)
        formJam.value = timeFormat.format(now)
        formStaff.value = ""
        formType.value = "Foto"
        formStatus.value = "Pending"
        editingSchedule.value = null
        _submitStatus.value = SubmitState.Idle
        lastAutofilledId = null
    }

    fun startEditing(schedule: Schedule) {
        editingSchedule.value = schedule
        formListingId.value = schedule.idListing
        formNamaMe.value = schedule.namaMe
        formLokasi.value = schedule.lokasi
        formTanggal.value = schedule.tanggal
        formJam.value = schedule.jam
        formStaff.value = schedule.staff
        formType.value = schedule.type
        formStatus.value = schedule.status
        _submitStatus.value = SubmitState.Idle
        lastAutofilledId = schedule.idListing
    }

    // Update settings in PreferenceManager
    fun saveSettings(url: String, sheetId: String, weeklyUrl: String) {
        preferenceManager.appsScriptUrl = url
        preferenceManager.spreadsheetId = sheetId
        preferenceManager.weeklyMeetingUrl = weeklyUrl
        appsScriptUrl.value = url
        spreadsheetId.value = sheetId
        weeklyMeetingUrl.value = weeklyUrl
        
        // After updating settings, let's sync immediately if url is provided
        if (url.isNotBlank()) {
            syncData()
        }
    }

    // Sync from Google Sheets (2-way: upload pending first, then fetch latest)
    fun syncData() {
        viewModelScope.launch {
            _syncStatus.value = SyncState.Loading
            
            // Step 1: Upload any unsynced local schedules
            var pendingUploaded = 0
            val uploadResult = repository.syncPendingSchedules()
            uploadResult.onSuccess { count ->
                pendingUploaded = count
            }
            
            // Step 2: Fetch all latest entries from Google Sheets
            val result = repository.syncFromGoogleSheets()
            result.onSuccess {
                val msg = if (pendingUploaded > 0) {
                    "Disinkronkan: $pendingUploaded data diunggah & data terbaru ditarik!"
                } else {
                    "Sinkronisasi selesai! Semua data sudah terbaru."
                }
                _syncStatus.value = SyncState.Success(msg)
                fetchYearlyIgPostingHistory()

                // Force background refresh of listing images/details for active schedules to keep them synchronized and fresh!
                allSchedules.value.map { it.idListing }.distinct().filter { it.isNotBlank() }.forEach { id ->
                    fetchListingImageIfNeeded(id, forceRefresh = true)
                }
            }.onFailure { err ->
                _syncStatus.value = SyncState.Error("Gagal mengambil data terbaru: ${err.localizedMessage ?: "Masalah koneksi"}")
            }
        }
    }

    // Silent real-time sync for background updates without disrupting UI states
    fun syncDataSilently() {
        viewModelScope.launch {
            repository.syncPendingSchedules()
            repository.syncFromGoogleSheets()
            syncChats()
            fetchYearlyIgPostingHistory()
        }
    }

    // Clear local database cache
    fun clearCache() {
        viewModelScope.launch {
            repository.clearLocal()
        }
    }

    // Submits schedule (handles both dynamic adding and updating/editing)
    fun submitSchedule() {
        val listingId = formListingId.value.trim()
        val namaMe = formNamaMe.value.trim()
        val lokasi = formLokasi.value.trim()
        val tanggal = formTanggal.value.trim()
        val jam = formJam.value.trim()
        val staff = formStaff.value.trim()
        val type = formType.value.trim()
        val status = formStatus.value.trim()

        if (listingId.isEmpty() && (namaMe.isEmpty() || lokasi.isEmpty())) {
            _submitStatus.value = SubmitState.Error("Masukkan ID Listing, atau isi Nama ME & Lokasi jika ID Listing dikosongkan.")
            return
        }
        if (tanggal.isEmpty() || jam.isEmpty() || staff.isEmpty()) {
            _submitStatus.value = SubmitState.Error("Tanggal, Jam, dan Staff wajib diisi!")
            return
        }

        val original = editingSchedule.value
        val isEditMode = original != null

        viewModelScope.launch {
            _submitStatus.value = SubmitState.Loading
            if (isEditMode && original != null) {
                val updatedSchedule = original.copy(
                    idListing = listingId,
                    namaMe = namaMe,
                    lokasi = lokasi,
                    tanggal = tanggal,
                    jam = jam,
                    staff = staff,
                    type = type,
                    status = status,
                    synced = false
                )
                val result = repository.updateSchedule(updatedSchedule, original)
                result.onSuccess {
                    _submitStatus.value = SubmitState.Success
                    resetForm()
                }.onFailure { err ->
                    _submitStatus.value = SubmitState.Error(
                        "Tersimpan di Lokal (Gagal sinkronisasi ke Sheets: ${err.localizedMessage ?: "Masalah jaringan"})"
                    )
                }
            } else {
                val schedule = Schedule(
                    idListing = listingId,
                    namaMe = namaMe,
                    lokasi = lokasi,
                    tanggal = tanggal,
                    jam = jam,
                    staff = staff,
                    type = type,
                    status = status,
                    synced = false
                )

                val result = repository.addSchedule(schedule)
                result.onSuccess {
                    _submitStatus.value = SubmitState.Success
                    resetForm()
                }.onFailure { err ->
                    _submitStatus.value = SubmitState.Error(
                        "Tersimpan di Lokal (Gagal sinkronisasi ke Sheets: ${err.localizedMessage ?: "Masalah jaringan"})"
                    )
                }
            }
        }
    }

    // Retry sending unsynced entries
    fun retryUnsyncedSchedules() {
        viewModelScope.launch {
            _syncStatus.value = SyncState.Loading
            val result = repository.syncPendingSchedules()
            result.onSuccess { count ->
                if (count > 0) {
                    _syncStatus.value = SyncState.Success("$count jadwal tertunda berhasil disinkronisasi!")
                    syncData()
                } else {
                    _syncStatus.value = SyncState.Success("Semua jadwal sudah tersinkronisasi.")
                }
            }.onFailure { err ->
                _syncStatus.value = SyncState.Error("Gagal sinkronisasi antrean: ${err.message}")
            }
        }
    }

    fun deleteSchedule(schedule: Schedule) {
        viewModelScope.launch {
            _syncStatus.value = SyncState.Loading
            val result = repository.deleteSchedule(schedule)
            result.onSuccess {
                _syncStatus.value = SyncState.Success("Jadwal '${schedule.idListing.ifBlank { schedule.namaMe }}' berhasil dihapus lokalan dan online!")
            }.onFailure { err ->
                _syncStatus.value = SyncState.Error("Terhapus di HP. Gagal menghapus dari Google Sheets online: ${err.message}")
            }
        }
    }

    fun deleteRow(schedule: Schedule) {
        viewModelScope.launch {
            _syncStatus.value = SyncState.Loading
            val result = repository.deleteRow(schedule)
            result.onSuccess {
                _syncStatus.value = SyncState.Success("Jadwal '${schedule.idListing.ifBlank { schedule.namaMe }}' berhasil dihapus lokalan dan online!")
            }.onFailure { err ->
                _syncStatus.value = SyncState.Error("Terhapus di HP. Gagal menghapus dari Google Sheets online: ${err.message}")
            }
        }
    }

    fun deleteEditFotoTask(task: com.example.data.EditFotoTask) {
        viewModelScope.launch {
            _syncStatus.value = SyncState.Loading
            val result = repository.deleteEditFotoTask(task)
            result.onSuccess {
                _syncStatus.value = SyncState.Success("Jadwal Edit '${task.idListing.ifBlank { task.namaMe }}' berhasil dihapus lokalan dan online!")
            }.onFailure { err ->
                _syncStatus.value = SyncState.Error("Terhapus di HP. Gagal menghapus dari Google Sheets online: ${err.message}")
            }
        }
    }

    fun dismissSyncStatus() {
        _syncStatus.value = SyncState.Idle
    }

    fun dismissSubmitStatus() {
        if (_submitStatus.value is SubmitState.Error) {
            _submitStatus.value = SubmitState.Idle
        }
    }

    // --- INSTAGRAM POSTING HISTORY ---
    data class IgPostingHistory(
        val isPosted: Boolean,
        val count: Int,
        val dates: List<String>
    )

    val yearlyIgPostingHistory = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val _yearlyIgSyncStatus = MutableStateFlow<SyncState>(SyncState.Idle)
    val yearlyIgSyncStatus: StateFlow<SyncState> = _yearlyIgSyncStatus.asStateFlow()

    fun fetchYearlyIgPostingHistory() {
        val baseUrl = appsScriptUrl.value
        if (baseUrl.isBlank()) return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _yearlyIgSyncStatus.value = SyncState.Loading
            try {
                val separator = if (baseUrl.contains("?")) "&" else "?"
                val url = "$baseUrl${separator}action=get_yearly_ig_posting_history"
                val response = apiService.getYearlyIgPostingHistory(url)
                if (response.status.lowercase() == "success") {
                    yearlyIgPostingHistory.value = response.history
                    _yearlyIgSyncStatus.value = SyncState.Success("Berhasil sinkronisasi postingan IG tahunan.")
                } else {
                    _yearlyIgSyncStatus.value = SyncState.Error("Server error: ${response.status}")
                }
            } catch (e: Exception) {
                _yearlyIgSyncStatus.value = SyncState.Error("Gagal mengambil data IG tahunan: ${e.localizedMessage}")
            }
        }
    }

    fun formatToSpreadsheetDate(dateStr: String): String {
        val clean = dateStr.trim()
        if (clean.isBlank()) return ""
        if (clean.contains(",") && clean.split(" ").size >= 3) {
            return clean
        }
        try {
            val sdfSource = if (clean.contains("-")) {
                if (clean.split("-")[0].length == 4) SimpleDateFormat("yyyy-MM-dd", Locale.US)
                else SimpleDateFormat("dd-MM-yyyy", Locale.US)
            } else if (clean.contains("/")) {
                SimpleDateFormat("dd/MM/yyyy", Locale.US)
            } else {
                null
            }
            if (sdfSource != null) {
                val dateObj = sdfSource.parse(clean)
                if (dateObj != null) {
                    val sdfDest = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
                    return sdfDest.format(dateObj)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return clean
    }

    fun parseToComparableDate(dateStr: String): String {
        try {
            val clean = dateStr.trim()
            if (clean.contains(",")) {
                val parts = clean.split(",")
                if (parts.size > 1) {
                    val datePart = parts[1].trim()
                    val sdf = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
                    val dateObj = sdf.parse(datePart)
                    if (dateObj != null) {
                        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dateObj)
                    }
                }
            }
            val sourceSdf = if (clean.contains("-")) {
                if (clean.split("-")[0].length == 4) SimpleDateFormat("yyyy-MM-dd", Locale.US)
                else SimpleDateFormat("dd-MM-yyyy", Locale.US)
            } else if (clean.contains("/")) {
                SimpleDateFormat("dd/MM/yyyy", Locale.US)
            } else {
                SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
            }
            val dateObj = sourceSdf.parse(clean)
            if (dateObj != null) {
                return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dateObj)
            }
        } catch (e: Exception) {
            // ignore
        }
        return "0000-00-00"
    }

    fun getIgPostingHistory(idListing: String): IgPostingHistory {
        val cleanId = idListing.trim().lowercase()
        val cleanIdNoDot = if (cleanId.endsWith(".0")) cleanId.substring(0, cleanId.length - 2) else cleanId
        if (cleanIdNoDot.isBlank()) return IgPostingHistory(false, 0, emptyList())
        
        val idInt = cleanIdNoDot.toIntOrNull()
        val dates = mutableSetOf<String>()
        
        // 1. Dari data history tahunan yang di-scrape dari google sheets
        var sheetsHistory: List<String>? = null
        for ((key, list) in yearlyIgPostingHistory.value) {
            val cleanKey = key.trim().lowercase()
            val cleanKeyNoDot = if (cleanKey.endsWith(".0")) cleanKey.substring(0, cleanKey.length - 2) else cleanKey
            val keyInt = cleanKeyNoDot.toIntOrNull()
            
            val isMatch = if (idInt != null && keyInt != null) {
                idInt == keyInt
            } else {
                cleanKeyNoDot == cleanIdNoDot
            }
            
            if (isMatch) {
                sheetsHistory = list
                break
            }
        }
        
        if (sheetsHistory != null) {
            sheetsHistory.forEach { date ->
                val formatted = formatToSpreadsheetDate(date)
                if (formatted.isNotBlank()) dates.add(formatted)
            }
        }
        
        // 2. Dari data lokal (allEditFotoTasks)
        allEditFotoTasks.value.forEach { task ->
            val taskId = task.idListing.trim().lowercase()
            val taskIdNoDot = if (taskId.endsWith(".0")) taskId.substring(0, taskId.length - 2) else taskId
            val taskIdInt = taskIdNoDot.toIntOrNull()
            
            val isMatch = if (idInt != null && taskIdInt != null) {
                idInt == taskIdInt
            } else {
                taskIdNoDot == cleanIdNoDot
            }
            
            if (isMatch) {
                if (task.postingIg || task.done) {
                    val dateStr = task.jadwalPosting.trim()
                    if (dateStr.isNotBlank() && !dateStr.contains("2024") && !dateStr.contains("2025")) {
                        val formatted = formatToSpreadsheetDate(dateStr)
                        if (formatted.isNotBlank()) dates.add(formatted)
                    }
                }
            }
        }
        
        val sortedDates = dates.toList().sortedBy { parseToComparableDate(it) }
        return IgPostingHistory(
            isPosted = sortedDates.isNotEmpty(),
            count = sortedDates.size,
            dates = sortedDates
        )
    }

    // --- TASK EDIT FOTO DASHBOARD EXPOSURE & HELPERS ---
    fun parseIndonesianDateToComparable(dateStr: String): String {
        val result = com.example.data.normalizeDate(dateStr)
        return if (result.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) result else "0000-00-00"
    }

    val allEditFotoTasks: StateFlow<List<com.example.data.EditFotoTask>> = repository.allEditFotoTasks
        .map { list ->
            list.sortedWith(
                compareByDescending<com.example.data.EditFotoTask> { parseIndonesianDateToComparable(it.jadwalPosting) }
                    .thenByDescending { it.no }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleEditFotoDone(task: com.example.data.EditFotoTask) {
        viewModelScope.launch {
            val updated = task.copy(done = !task.done, synced = false)
            repository.updateEditFotoTask(updated)
        }
    }

    fun toggleEditFotoPostingIg(task: com.example.data.EditFotoTask) {
        viewModelScope.launch {
            val updated = task.copy(postingIg = !task.postingIg, synced = false)
            repository.updateEditFotoTask(updated)
        }
    }

    fun updateEditFotoTaskDirectly(task: com.example.data.EditFotoTask) {
        viewModelScope.launch {
            _syncStatus.value = SyncState.Loading
            val result = repository.updateEditFotoTask(task)
            result.onSuccess {
                _syncStatus.value = SyncState.Success("Task Edit Jadwal Berhasil Diperbarui!")
                syncDataSilently()
            }.onFailure { err ->
                _syncStatus.value = SyncState.Error("Simpan Lokal Sukses (Sheets tertunda: ${err.message})")
                syncDataSilently()
            }
        }
    }


    fun syncChats() {
        val url = getChatAppsScriptUrl()
        if (url.isBlank()) return
        
        viewModelScope.launch {
            if (_chatSyncing.value) return@launch
            _chatSyncing.value = true
            try {
                // 1. Upload any unsynced local messages
                syncPendingChats()

                // 2. Fetch latest chats from Google Sheets
                val responseBody = apiService.getSchedules(url)
                val jsonString = responseBody.string().trim()
                
                val moshi = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                
                val chatList = mutableListOf<ChatMessage>()
                var isFallbackDetected = false

                if (jsonString.startsWith("{")) {
                    val adapter = moshi.adapter(com.example.network.CombinedSheetResponse::class.java)
                    val response = adapter.fromJson(jsonString)
                    if (response != null) {
                        val schedules = response.schedules
                        val hasRealSchedules = schedules.any { it.lokasi.isNotBlank() && it.lokasi != "CHAT_MESSAGE" }
                        if (hasRealSchedules) {
                            isFallbackDetected = true
                        } else {
                            schedules.forEach {
                                chatList.add(mapSheetScheduleToChatMessage(it))
                            }
                        }
                    }
                } else if (jsonString.startsWith("[")) {
                    val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.network.SheetSchedule::class.java)
                    val adapter = moshi.adapter<List<com.example.network.SheetSchedule>>(type)
                    val list = adapter.fromJson(jsonString)
                    if (list != null) {
                        val hasRealSchedules = list.any { it.lokasi.isNotBlank() && it.lokasi != "CHAT_MESSAGE" }
                        if (hasRealSchedules) {
                            isFallbackDetected = true
                        } else {
                            list.forEach {
                                chatList.add(mapSheetScheduleToChatMessage(it))
                            }
                        }
                    }
                }

                _isChatSheetMissing.value = isFallbackDetected

                if (!isFallbackDetected) {
                    val unsynced = db.chatMessageDao().getAllMessages().first().filter { !it.synced }
                    db.chatMessageDao().updateChatMessagesTransaction(chatList, unsynced)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatSync", "Failed to sync chats", e)
            } finally {
                _chatSyncing.value = false
            }
        }
    }

    private fun getChatAppsScriptUrl(): String {
        val baseUrl = preferenceManager.appsScriptUrl
        if (baseUrl.isBlank()) return ""
        val separator = if (baseUrl.contains("?")) "&" else "?"
        return "$baseUrl${separator}sheetName=Chat"
    }

    private fun mapSheetScheduleToChatMessage(it: com.example.network.SheetSchedule): ChatMessage {
        val messageText = it.idListing.trim()
        val sender = it.namaMe.trim()
        val device = it.staff.trim().ifBlank { "Unknown" }
        
        val timestamp = it.type.toLongOrNull() ?: try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            val parsedDate = sdf.parse("${it.tanggal.trim()} ${it.jam.trim()}")
            parsedDate?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        return ChatMessage(
            no = it.no,
            senderName = sender,
            message = messageText,
            timestamp = timestamp,
            deviceModel = device,
            synced = true
        )
    }

    private suspend fun syncPendingChats() {
        val url = getChatAppsScriptUrl()
        if (url.isBlank()) return
        
        val unsynced = db.chatMessageDao().getAllMessages().first().filter { !it.synced }
        for (msg in unsynced) {
            try {
                val sdfDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val sdfTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                val dateStr = sdfDate.format(java.util.Date(msg.timestamp))
                val timeStr = sdfTime.format(java.util.Date(msg.timestamp))

                val isEdit = msg.no > 0
                val sheetModel = com.example.network.SheetSchedule(
                    no = if (isEdit) msg.no else 0,
                    idListing = msg.message,
                    namaMe = msg.senderName,
                    lokasi = "CHAT_MESSAGE",
                    tanggal = dateStr,
                    jam = timeStr,
                    staff = msg.deviceModel,
                    type = msg.timestamp.toString(),
                    status = "active",
                    action = if (isEdit) "edit" else "add",
                    sheetName = "Chat"
                )
                val result = apiService.addSchedule(url, sheetModel)
                val statusLow = result.status.lowercase()
                val msgLow = result.message.lowercase()
                if (statusLow == "success" || statusLow == "ok" || msgLow == "success" || msgLow == "ok") {
                    val remoteNo = if (!isEdit && result.row != null && result.row > 4) result.row - 4 else msg.no
                    db.chatMessageDao().insertMessage(msg.copy(synced = true, no = remoteNo))
                }
            } catch (e: Exception) {
                // Keep local
            }
        }
    }

    fun sendChatMessage(messageText: String) {
        if (messageText.isBlank()) return
        viewModelScope.launch {
            val sender = getSenderName()
            val deviceModel = android.os.Build.MODEL ?: "Unknown"
            val chatMsg = ChatMessage(
                senderName = sender,
                message = messageText,
                timestamp = System.currentTimeMillis(),
                deviceModel = deviceModel,
                synced = false
            )
            db.chatMessageDao().insertMessage(chatMsg)
            syncChats()
        }
    }

    fun editChatMessage(message: ChatMessage, newText: String) {
        if (newText.isBlank()) return
        viewModelScope.launch {
            val updated = message.copy(message = newText, synced = false)
            db.chatMessageDao().updateMessage(updated)
            syncChats()
        }
    }

    fun deleteChatMessage(messageId: Int) {
        viewModelScope.launch {
            val msgList = db.chatMessageDao().getAllMessages().first()
            val message = msgList.find { it.id == messageId }
            
            // Delete locally
            db.chatMessageDao().deleteMessageById(messageId)

            val url = getChatAppsScriptUrl()
            if (message != null && url.isNotBlank()) {
                try {
                    val sheetModel = com.example.network.SheetSchedule(
                        no = message.no,
                        idListing = message.message,
                        namaMe = message.senderName,
                        lokasi = "CHAT_MESSAGE",
                        tanggal = "",
                        jam = "",
                        staff = message.deviceModel,
                        type = message.timestamp.toString(),
                        status = "deleted",
                        action = "delete",
                        sheetName = "Chat"
                    )
                    apiService.addSchedule(url, sheetModel)
                } catch (e: Exception) {
                    // ignore network failure for deletion
                }
                syncChats()
            }
        }
    }

    fun getSenderName(): String {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("chat_prefs", android.content.Context.MODE_PRIVATE)
        val override = prefs.getString("sender_role_override", null)
        if (override != null) return override
        
        return "Raffa"
    }

    fun setSenderRoleOverride(role: String?) {
        val context = getApplication<Application>()
        context.getSharedPreferences("chat_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("sender_role_override", role)
            .apply()
    }
}

sealed interface SyncState {
    object Idle : SyncState
    object Loading : SyncState
    data class Success(val message: String) : SyncState
    data class Error(val message: String) : SyncState
}

sealed interface SubmitState {
    object Idle : SubmitState
    object Loading : SubmitState
    object Success : SubmitState
    data class Error(val message: String) : SubmitState
}

enum class ScheduleCategory(val displayName: String) {
    SEMUA("Semua"),
    AKTIF("Jadwal Aktif"),
    SELESAI("Jadwal Selesai"),
    NON_AKTIF("Jadwal Non-Aktif")
}

private data class AutofillData(
    val currentId: String,
    val agent: AgentInfo?,
    val title: String?,
    val desc: String?
)
