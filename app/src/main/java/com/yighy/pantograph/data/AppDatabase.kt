package com.yighy.pantograph.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yighy.pantograph.BuildConfig

@Database(
    entities = [ProjectEntity::class, LayerEntity::class, StrokeEntity::class, CustomBrushEntity::class, BrushFolderEntity::class],
    version = 25,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Pre-1.0, a schema change with no migration still throws the database away and
        // rebuilds it, the trade SchemaPolicy describes. A change that is only new columns with
        // defaults costs one statement each to carry over, though, and people's drawings are in
        // that file now - so those get a real migration, and the fallback is left for the
        // changes that cannot be expressed that cheaply.

        /** A blend mode and an alpha lock for every layer, Normal and off for those already there. */
        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE layers ADD COLUMN blendMode TEXT NOT NULL DEFAULT 'Normal'")
                db.execSQL("ALTER TABLE layers ADD COLUMN isAlphaLocked INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "drawing_database"
                ).addMigrations(MIGRATION_24_25)

                if (SchemaPolicy.allowsDestructiveFallback(BuildConfig.VERSION_NAME)) {
                    builder.fallbackToDestructiveMigration(dropAllTables = true)
                }

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }
    }
}
