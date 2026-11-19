package app.clearline.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Store(context: Context) {
    private val prefs = context.getSharedPreferences("clearline", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadAccount(): SipAccount {
        val json = prefs.getString("account", null) ?: return SipAccount()
        return runCatching { gson.fromJson(json, SipAccount::class.java) }.getOrDefault(SipAccount())
    }

    fun saveAccount(account: SipAccount) {
        prefs.edit().putString("account", gson.toJson(account)).apply()
    }

    fun loadContacts(): List<Contact> {
        val json = prefs.getString("contacts", null) ?: return seedContacts()
        val type = object : TypeToken<List<Contact>>() {}.type
        return runCatching { gson.fromJson<List<Contact>>(json, type) }.getOrElse { seedContacts() }
    }

    fun saveContacts(contacts: List<Contact>) {
        prefs.edit().putString("contacts", gson.toJson(contacts)).apply()
    }

    fun loadRecents(): List<CallRecord> {
        val json = prefs.getString("recents", null) ?: return emptyList()
        val type = object : TypeToken<List<CallRecord>>() {}.type
        return runCatching { gson.fromJson<List<CallRecord>>(json, type) }.getOrDefault(emptyList())
    }

    fun saveRecents(recents: List<CallRecord>) {
        prefs.edit().putString("recents", gson.toJson(recents.take(100))).apply()
    }

    private fun seedContacts(): List<Contact> {
        val seeded = listOf(
            Contact(name = "Echo Lab", uri = "echo@demo.clearline", note = "Demo loopback"),
            Contact(name = "Desk Phone", uri = "1001@office.local", note = "Office PBX"),
            Contact(name = "Night Desk", uri = "ops@night.clearline", note = "On-call"),
        )
        saveContacts(seeded)
        return seeded
    }
}
