package com.aduilio.mytasks.fragment

import android.os.Bundle
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.aduilio.mytasks.R
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging

class PreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)


        val dailyNotification = findPreference<SwitchPreferenceCompat>("daily_notification")
        dailyNotification?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->

            if (newValue.toString().toBoolean()) {
                Firebase.messaging.subscribeToTopic("daily_notification").addOnCompleteListener {
                    Log.e("fcm", "Subscription")
                }
            } else {
                Firebase.messaging.unsubscribeFromTopic("daily_notification").addOnCompleteListener {
                    Log.e("fcm", "Unsubscription")
                }
            }

            true
        }

        val dateFormatPreference = findPreference<ListPreference>("date_format")
        dateFormatPreference?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
    }
}