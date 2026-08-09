package org.omnilex.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.omnilex.data.model.*

@Database(entities = [LexicalEntry::class, Sense::class, LexicalRelationship::class, LexicalSource::class], version = 1, exportSchema = true)
@TypeConverters(RoomConverters::class)
abstract class OmniLexDatabase : RoomDatabase() { abstract fun omniLexDao(): OmniLexDao }
