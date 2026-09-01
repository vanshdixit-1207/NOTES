package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [NoteEntity::class, FolderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: NotesDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): NotesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotesDatabase::class.java,
                    "ios_notes_database"
                )
                .addCallback(NotesDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class NotesDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.folderDao(), database.noteDao())
                    }
                }
            }

            private suspend fun populateInitialData(folderDao: FolderDao, noteDao: NoteDao) {
                // Initial iOS Folders
                val defaultFolders = listOf(
                    FolderEntity(id = 1, name = "Notes", iconName = "folder", colorHex = "#E3A108", orderIndex = 0, isSystem = true),
                    FolderEntity(id = 2, name = "Quick Notes", iconName = "flash", colorHex = "#FF9500", orderIndex = 1, isSystem = false),
                    FolderEntity(id = 3, name = "Personal", iconName = "person", colorHex = "#34C759", orderIndex = 2, isSystem = false),
                    FolderEntity(id = 4, name = "Work & Projects", iconName = "briefcase", colorHex = "#007AFF", orderIndex = 3, isSystem = false),
                    FolderEntity(id = 5, name = "Travel & Places", iconName = "airplane", colorHex = "#AF52DE", orderIndex = 4, isSystem = false)
                )
                folderDao.insertFolders(defaultFolders)

                val currentTime = System.currentTimeMillis()
                val oneHourAgo = currentTime - 3600 * 1000
                val yesterday = currentTime - 86400 * 1000
                val threeDaysAgo = currentTime - 3 * 86400 * 1000
                val lastWeek = currentTime - 7 * 86400 * 1000

                val sampleNotes = listOf(
                    NoteEntity(
                        title = "Welcome to iOS Notes ✨",
                        content = "Enjoy the clean Cupertino experience designed with Apple's iconic San Francisco typography, grouped list cards, warm amber accents, and fluid transitions.\n\n• Pinned Notes & Folder Organization\n• Rich Formatting & Interactive Checklists\n• Fast Search & Filter Tokens\n• Lock Notes for Privacy\n• Grid & List View Toggle\n• Trash Recovery",
                        folderId = 1,
                        isPinned = true,
                        isLocked = false,
                        checklistsJson = """[{"id":"c1","text":"Explore folder navigation","isChecked":true},{"id":"c2","text":"Try interactive checklist items","isChecked":true},{"id":"c3","text":"Toggle Grid / List view mode","isChecked":false},{"id":"c4","text":"Create a new quick note","isChecked":false}]""",
                        createdAt = currentTime,
                        updatedAt = currentTime
                    ),
                    NoteEntity(
                        title = "Weekly Grocery & Essentials 🛒",
                        content = "Items to pick up for the weekend brunch and meal prep:\n\nRemember to check farmer's market for fresh sourdough!",
                        folderId = 3,
                        isPinned = true,
                        isLocked = false,
                        checklistsJson = """[{"id":"g1","text":"Organic Avocados & Sourdough","isChecked":true},{"id":"g2","text":"Almond milk & Greek yogurt","isChecked":true},{"id":"g3","text":"Cold brew coffee beans","isChecked":false},{"id":"g4","text":"Fresh blueberries & strawberries","isChecked":false},{"id":"g5","text":"Extra virgin olive oil","isChecked":false}]""",
                        createdAt = oneHourAgo,
                        updatedAt = oneHourAgo
                    ),
                    NoteEntity(
                        title = "App Design System & Architecture",
                        content = "Key Principles:\n\n1. Hierarchy: Bold SF Pro titles with distinct visual rhythm\n2. Grouped Cards: 10dp radius with high contrast surfaces\n3. Responsive insets & dynamic safe areas\n4. Haptic feedback on interactive checkmarks\n5. Room local persistence with reactive Flow streams",
                        folderId = 4,
                        isPinned = false,
                        isLocked = false,
                        createdAt = yesterday,
                        updatedAt = yesterday
                    ),
                    NoteEntity(
                        title = "Kyoto & Tokyo Itinerary 🗾",
                        content = "Day 1: Arrive in Narita -> Shinkansen to Kyoto\nDay 2: Fushimi Inari Shrine at sunrise & Arashiyama Bamboo Grove\nDay 3: Gion evening walk & tea ceremony\nDay 4: Return to Tokyo (Shibuya, Shinjuku night market)\nDay 5: TeamLab Planets & Akihabara tech tour",
                        folderId = 5,
                        isPinned = false,
                        isLocked = false,
                        createdAt = threeDaysAgo,
                        updatedAt = threeDaysAgo
                    ),
                    NoteEntity(
                        title = "Favorite Quotes & Thoughts",
                        content = "\"Simplicity is about subtracting the obvious and adding the meaningful.\"\n— John Maeda\n\n\"Design is not just what it looks like and feels like. Design is how it works.\"\n— Steve Jobs",
                        folderId = 2,
                        isPinned = false,
                        isLocked = false,
                        createdAt = lastWeek,
                        updatedAt = lastWeek
                    )
                )
                noteDao.insertNotes(sampleNotes)
            }
        }
    }
}
