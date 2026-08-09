package org.omnilex.data.local

import androidx.room.TypeConverter
import org.omnilex.data.model.ImportConflictStatus
import org.omnilex.data.model.RelationshipType

class RoomConverters {
    @TypeConverter fun relationshipTypeToString(value: RelationshipType) = value.name
    @TypeConverter fun stringToRelationshipType(value: String) = RelationshipType.valueOf(value)
    @TypeConverter fun importConflictStatusToString(value: ImportConflictStatus) = value.name
    @TypeConverter fun stringToImportConflictStatus(value: String) = ImportConflictStatus.valueOf(value)
}
