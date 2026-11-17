package com.sda.attendance.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sda.attendance.data.model.AttendanceRecord
import com.sda.attendance.data.model.AttendanceSession
import com.sda.attendance.data.model.DefaultStudents
import com.sda.attendance.data.model.Student

class AttendanceDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val gson = Gson()

    companion object {
        const val DATABASE_NAME = "attendance_master.db"
        const val DATABASE_VERSION = 1

        // Students table
        const val TABLE_STUDENTS = "students"
        const val COL_ROLL = "roll_number"
        const val COL_FULL_NAME = "full_name"
        const val COL_DISPLAY_NAME = "display_name"
        const val COL_IS_GATE = "is_gate"
        const val COL_PHONE = "phone_number"
        const val COL_GENDER = "gender"
        const val COL_CATEGORY = "category"

        // Sessions table
        const val TABLE_SESSIONS = "sessions"
        const val COL_SESSION_ID = "id"
        const val COL_DATE = "date"
        const val COL_TIME = "time"
        const val COL_SESSION_NAME = "session_name"
        const val COL_RECORDS_JSON = "records_json"
        const val COL_NOTES = "notes"
        const val COL_CREATED_AT = "created_at"
        const val COL_UPDATED_AT = "updated_at"
        const val COL_IS_SYNCED = "is_synced"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createStudentsTable = """
            CREATE TABLE $TABLE_STUDENTS (
                $COL_ROLL TEXT PRIMARY KEY,
                $COL_FULL_NAME TEXT NOT NULL,
                $COL_DISPLAY_NAME TEXT NOT NULL,
                $COL_IS_GATE INTEGER NOT NULL,
                $COL_PHONE TEXT,
                $COL_GENDER TEXT,
                $COL_CATEGORY TEXT
            )
        """.trimIndent()

        val createSessionsTable = """
            CREATE TABLE $TABLE_SESSIONS (
                $COL_SESSION_ID TEXT PRIMARY KEY,
                $COL_DATE TEXT NOT NULL,
                $COL_TIME TEXT NOT NULL,
                $COL_SESSION_NAME TEXT NOT NULL,
                $COL_RECORDS_JSON TEXT NOT NULL,
                $COL_NOTES TEXT,
                $COL_CREATED_AT INTEGER NOT NULL,
                $COL_UPDATED_AT INTEGER NOT NULL,
                $COL_IS_SYNCED INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()

        db.execSQL(createStudentsTable)
        db.execSQL(createSessionsTable)

        // Seed initial 20 students
        DefaultStudents.list.forEach { s ->
            val cv = ContentValues().apply {
                put(COL_ROLL, s.rollNumber)
                put(COL_FULL_NAME, s.fullName)
                put(COL_DISPLAY_NAME, s.displayName)
                put(COL_IS_GATE, if (s.isGate) 1 else 0)
                put(COL_PHONE, s.phoneNumber)
                put(COL_GENDER, s.gender)
                put(COL_CATEGORY, s.category)
            }
            db.insert(TABLE_STUDENTS, null, cv)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migration logic if needed
    }

    // -------------------------------------------------------------
    // STUDENT CRUD
    // -------------------------------------------------------------
    fun getAllStudents(): List<Student> {
        val list = mutableListOf<Student>()
        val db = readableDatabase
        val cursor = db.query(TABLE_STUDENTS, null, null, null, null, null, "$COL_ROLL ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Student(
                        rollNumber = it.getString(it.getColumnIndexOrThrow(COL_ROLL)),
                        fullName = it.getString(it.getColumnIndexOrThrow(COL_FULL_NAME)),
                        displayName = it.getString(it.getColumnIndexOrThrow(COL_DISPLAY_NAME)),
                        isGate = it.getInt(it.getColumnIndexOrThrow(COL_IS_GATE)) == 1,
                        phoneNumber = it.getString(it.getColumnIndexOrThrow(COL_PHONE)) ?: "",
                        gender = it.getString(it.getColumnIndexOrThrow(COL_GENDER)) ?: "",
                        category = it.getString(it.getColumnIndexOrThrow(COL_CATEGORY)) ?: ""
                    )
                )
            }
        }
        return list
    }

    fun upsertStudent(student: Student) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_ROLL, student.rollNumber)
            put(COL_FULL_NAME, student.fullName)
            put(COL_DISPLAY_NAME, student.displayName)
            put(COL_IS_GATE, if (student.isGate) 1 else 0)
            put(COL_PHONE, student.phoneNumber)
            put(COL_GENDER, student.gender)
            put(COL_CATEGORY, student.category)
        }
        db.insertWithOnConflict(TABLE_STUDENTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateAllStudents(students: List<Student>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            students.forEach { s ->
                val cv = ContentValues().apply {
                    put(COL_FULL_NAME, s.fullName)
                    put(COL_DISPLAY_NAME, s.displayName)
                    put(COL_IS_GATE, if (s.isGate) 1 else 0)
                    put(COL_PHONE, s.phoneNumber)
                }
                db.update(TABLE_STUDENTS, cv, "$COL_ROLL = ?", arrayOf(s.rollNumber))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteStudent(rollNumber: String) {
        val db = writableDatabase
        db.delete(TABLE_STUDENTS, "$COL_ROLL = ?", arrayOf(rollNumber))
    }

    fun replaceAllStudents(students: List<Student>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_STUDENTS, null, null)
            students.forEach { s ->
                val cv = ContentValues().apply {
                    put(COL_ROLL, s.rollNumber)
                    put(COL_FULL_NAME, s.fullName)
                    put(COL_DISPLAY_NAME, s.displayName)
                    put(COL_IS_GATE, if (s.isGate) 1 else 0)
                    put(COL_PHONE, s.phoneNumber)
                    put(COL_GENDER, s.gender)
                    put(COL_CATEGORY, s.category)
                }
                db.insert(TABLE_STUDENTS, null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun clearAllStudents() {
        val db = writableDatabase
        db.delete(TABLE_STUDENTS, null, null)
    }

    fun resetStudentsToDefault() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_STUDENTS, null, null)
            DefaultStudents.list.forEach { s ->
                val cv = ContentValues().apply {
                    put(COL_ROLL, s.rollNumber)
                    put(COL_FULL_NAME, s.fullName)
                    put(COL_DISPLAY_NAME, s.displayName)
                    put(COL_IS_GATE, if (s.isGate) 1 else 0)
                    put(COL_PHONE, s.phoneNumber)
                    put(COL_GENDER, s.gender)
                    put(COL_CATEGORY, s.category)
                }
                db.insert(TABLE_STUDENTS, null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // -------------------------------------------------------------
    // SESSIONS CRUD
    // -------------------------------------------------------------
    fun saveSession(session: AttendanceSession) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_SESSION_ID, session.id)
            put(COL_DATE, session.date)
            put(COL_TIME, session.time)
            put(COL_SESSION_NAME, session.sessionName)
            put(COL_RECORDS_JSON, gson.toJson(session.records))
            put(COL_NOTES, session.notes)
            put(COL_CREATED_AT, session.createdAt)
            put(COL_UPDATED_AT, session.updatedAt)
            put(COL_IS_SYNCED, if (session.isSynced) 1 else 0)
        }
        db.insertWithOnConflict(TABLE_SESSIONS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllSessions(): List<AttendanceSession> {
        val list = mutableListOf<AttendanceSession>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SESSIONS, null, null, null, null, null,
            "$COL_CREATED_AT DESC"
        )
        val typeToken = object : TypeToken<Map<String, AttendanceRecord>>() {}.type
        cursor.use {
            while (it.moveToNext()) {
                val recordsJson = it.getString(it.getColumnIndexOrThrow(COL_RECORDS_JSON))
                val records: Map<String, AttendanceRecord> = try {
                    gson.fromJson(recordsJson, typeToken) ?: emptyMap()
                } catch (e: Exception) {
                    emptyMap()
                }

                list.add(
                    AttendanceSession(
                        id = it.getString(it.getColumnIndexOrThrow(COL_SESSION_ID)),
                        date = it.getString(it.getColumnIndexOrThrow(COL_DATE)),
                        time = it.getString(it.getColumnIndexOrThrow(COL_TIME)),
                        sessionName = it.getString(it.getColumnIndexOrThrow(COL_SESSION_NAME)),
                        records = records,
                        notes = it.getString(it.getColumnIndexOrThrow(COL_NOTES)) ?: "",
                        createdAt = it.getLong(it.getColumnIndexOrThrow(COL_CREATED_AT)),
                        updatedAt = it.getLong(it.getColumnIndexOrThrow(COL_UPDATED_AT)),
                        isSynced = it.getInt(it.getColumnIndexOrThrow(COL_IS_SYNCED)) == 1
                    )
                )
            }
        }
        return list
    }

    fun getSessionById(id: String): AttendanceSession? {
        val db = readableDatabase
        val cursor = db.query(TABLE_SESSIONS, null, "$COL_SESSION_ID = ?", arrayOf(id), null, null, null)
        val typeToken = object : TypeToken<Map<String, AttendanceRecord>>() {}.type
        cursor.use {
            if (it.moveToFirst()) {
                val recordsJson = it.getString(it.getColumnIndexOrThrow(COL_RECORDS_JSON))
                val records: Map<String, AttendanceRecord> = try {
                    gson.fromJson(recordsJson, typeToken) ?: emptyMap()
                } catch (e: Exception) {
                    emptyMap()
                }
                return AttendanceSession(
                    id = it.getString(it.getColumnIndexOrThrow(COL_SESSION_ID)),
                    date = it.getString(it.getColumnIndexOrThrow(COL_DATE)),
                    time = it.getString(it.getColumnIndexOrThrow(COL_TIME)),
                    sessionName = it.getString(it.getColumnIndexOrThrow(COL_SESSION_NAME)),
                    records = records,
                    notes = it.getString(it.getColumnIndexOrThrow(COL_NOTES)) ?: "",
                    createdAt = it.getLong(it.getColumnIndexOrThrow(COL_CREATED_AT)),
                    updatedAt = it.getLong(it.getColumnIndexOrThrow(COL_UPDATED_AT)),
                    isSynced = it.getInt(it.getColumnIndexOrThrow(COL_IS_SYNCED)) == 1
                )
            }
        }
        return null
    }

    fun deleteSession(sessionId: String) {
        val db = writableDatabase
        db.delete(TABLE_SESSIONS, "$COL_SESSION_ID = ?", arrayOf(sessionId))
    }

    fun markSessionSynced(sessionId: String, isSynced: Boolean = true) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_IS_SYNCED, if (isSynced) 1 else 0)
        }
        db.update(TABLE_SESSIONS, cv, "$COL_SESSION_ID = ?", arrayOf(sessionId))
    }
}
