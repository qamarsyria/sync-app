package com.sync.app

import android.content.Context

object Prefs {
    private const val NAME = "sync_prefs"
    private const val KEY_MY_TOPIC = "my_topic"
    private const val KEY_PARTNER_TOPIC = "partner_topic"
    private const val KEY_PASSWORD = "password"
    private const val KEY_IS_SETUP = "is_setup"

    fun setTopics(ctx: Context, my: String, partner: String) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_MY_TOPIC, my)
            .putString(KEY_PARTNER_TOPIC, partner)
            .apply()
    }

    fun myTopic(ctx: Context): String? =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(KEY_MY_TOPIC, null)

    fun partnerTopic(ctx: Context): String? =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(KEY_PARTNER_TOPIC, null)

    fun setPassword(ctx: Context, pass: String) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_PASSWORD, pass)
            .apply()
    }

    fun password(ctx: Context): String? =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(KEY_PASSWORD, null)

    fun setSetupDone(ctx: Context, done: Boolean) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_IS_SETUP, done)
            .apply()
    }

    fun isSetupDone(ctx: Context): Boolean =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getBoolean(KEY_IS_SETUP, false)
}
