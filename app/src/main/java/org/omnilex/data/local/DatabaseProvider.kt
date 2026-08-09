package org.omnilex.data.local

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    @Volatile
    private var INSTANCE: OmniLexDatabase? = null

    fun getDatabase(context: Context): OmniLexDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                OmniLexDatabase::class.java,
                "omnilex_database"
            ).build()

            INSTANCE = instance
            instance
        }
    }
}