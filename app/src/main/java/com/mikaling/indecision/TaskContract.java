package com.mikaling.indecision;

import android.provider.BaseColumns;

public class TaskContract {

    private TaskContract() {    }

    public static final class TaskEntry implements BaseColumns {
        // Name of table
        public static final String TABLE_NAME = "taskList";
        // "Name" column of table
        public static final String COLUMN_NAME = "name";
        // Timestamp column
        public static final String COLUMN_TIMESTAMP = "timestamp";
    }
}
