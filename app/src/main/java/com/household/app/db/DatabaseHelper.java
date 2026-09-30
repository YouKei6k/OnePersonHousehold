package com.household.app.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * [역할] 앱 전체에서 사용하는 SQLite 데이터베이스 헬퍼. 모든 DAO가 이 클래스를 통해 DB에 접근한다.
 * DB_설계서.md 의 ERD를 그대로 반영한다.
 *
 * 09.18 : USER 테이블(회원) 실제 구현
 * 09.21 : HOME_FAVORITE 테이블(홈 화면 즐겨찾기, REQ-014) 실제 구현
 * 그 외 나머지 테이블은 추후 화면 개발일(09.24~09.28)에 맞춰
 * CRUD 코드를 추가할 예정이라 스키마만 미리 생성해 둔다.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Logcat에서 이 태그로 필터링하면 DB 생성/업그레이드 관련 로그만 모아볼 수 있다.
    private static final String TAG = "DatabaseHelper";

    private static final String DATABASE_NAME = "household.db";
    private static final int DATABASE_VERSION = 5; // 반복일정 묶음 삭제를 위한 SCHEDULE.recur_group_id 추가

    private final Context context; // CSV 자산 로딩(TrashRegionLoader)에 필요

    // ===================== 테이블 / 컬럼명 상수 =====================
    // USER (회원) - REQ-001, REQ-002, REQ-003
    public static final String TABLE_USER = "USER";
    public static final String COL_USER_SEQ = "user_seq";      // PK
    public static final String COL_USER_ID = "user_id";        // 로그인 아이디
    public static final String COL_USER_PW = "user_pw";        // 비밀번호
    public static final String COL_USER_NAME = "user_name";    // 이름
    public static final String COL_USER_MAIL = "user_mail";    // 연락처 또는 이메일
    public static final String COL_USER_PROFILE = "user_profile"; // 프로필 이미지 경로
    public static final String COL_CREATED_AT = "created_at";

    // HOME_FAVORITE (홈 화면 즐겨찾기) - REQ-014 비고: "즐겨찾기 할 수 있음"
    // 4개 도메인 테이블(SCHEDULE/UTILITY_BILL/INGREDIENT/TRASH_DISPOSAL)에서
    // 나온 알림 항목 중 사용자가 즐겨찾기 한 것을 (출처테이블, 원본 PK) 쌍으로 기록한다.
    public static final String TABLE_HOME_FAVORITE = "HOME_FAVORITE";
    public static final String COL_FAV_USER_SEQ = "user_seq";
    public static final String COL_FAV_SOURCE_TYPE = "source_type"; // SCHEDULE/UTILITY_BILL/INGREDIENT/TRASH_DISPOSAL
    public static final String COL_FAV_SOURCE_ID = "source_id";     // 각 테이블의 PK 값

    public DatabaseHelper(Context context) {
        // 컨텍스트, DB이름, 커서 팩토리(기본값 null), DB 버전
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // household.db 파일이 없어서(최초 설치 또는 앱 데이터 삭제 후) 새로 만들어질 때 딱 한 번 호출된다.
        Log.d(TAG, "onCreate() 시작 - 전체 테이블을 새로 생성합니다.");

        // ---------- USER : 오늘(09.18) 실제 구현 대상 ----------
        db.execSQL(
                "CREATE TABLE " + TABLE_USER + " (" +
                        COL_USER_SEQ + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_USER_ID + " TEXT NOT NULL UNIQUE, " +
                        COL_USER_PW + " TEXT NOT NULL, " +
                        COL_USER_NAME + " TEXT NOT NULL, " +
                        COL_USER_MAIL + " TEXT NOT NULL, " +
                        COL_USER_PROFILE + " TEXT, " +
                        COL_CREATED_AT + " TEXT NOT NULL" +
                        ");"
        );

        // ---------- 아래는 09.24 ~ 09.28 개발 예정 테이블 (스키마만 선(先) 생성) ----------

        // SCHEDULE (생활일정) - REQ-004, 005, 006
        db.execSQL(
                "CREATE TABLE SCHEDULE (" +
                        "schedule_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_seq INTEGER NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "detail TEXT, " +
                        "date_start TEXT, " +
                        "date_end TEXT, " +
                        "is_recurring INTEGER DEFAULT 0, " +
                        "recurring_type TEXT, " +
                        "is_done INTEGER DEFAULT 0, " +
                        "recur_group_id INTEGER, " + // 같은 반복일정으로 만들어진 행들을 묶는 값 (반복 전체 삭제용)
                        "FOREIGN KEY(user_seq) REFERENCES " + TABLE_USER + "(" + COL_USER_SEQ + ")" +
                        ");"
        );

        // SCHEDULE_NOTI (일정 알림) - REQ-005
        db.execSQL(
                "CREATE TABLE SCHEDULE_NOTI (" +
                        "noti_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "schedule_id INTEGER NOT NULL, " +
                        "dday INTEGER NOT NULL, " +
                        "FOREIGN KEY(schedule_id) REFERENCES SCHEDULE(schedule_id)" +
                        ");"
        );

        // CALENDAR_STICKER (캘린더 스티커) - REQ-004
        db.execSQL(
                "CREATE TABLE CALENDAR_STICKER (" +
                        "sticker_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_seq INTEGER NOT NULL, " +
                        "date TEXT NOT NULL, " +
                        "sticker_type TEXT NOT NULL, " +
                        "FOREIGN KEY(user_seq) REFERENCES " + TABLE_USER + "(" + COL_USER_SEQ + ")" +
                        ");"
        );

        // UTILITY_BILL (공과금) - REQ-007, 008
        db.execSQL(
                "CREATE TABLE UTILITY_BILL (" +
                        "bill_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_seq INTEGER NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "payment_date TEXT NOT NULL, " +
                        "dday INTEGER, " +
                        "is_paid INTEGER DEFAULT 0, " +
                        "FOREIGN KEY(user_seq) REFERENCES " + TABLE_USER + "(" + COL_USER_SEQ + ")" +
                        ");"
        );

        // INGREDIENT (식재료) - REQ-007, 009
        db.execSQL(
                "CREATE TABLE INGREDIENT (" +
                        "ingredient_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_seq INTEGER NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "purchase_date TEXT, " +
                        "expire_date TEXT, " +
                        "dday INTEGER, " +
                        "is_done INTEGER DEFAULT 0, " +
                        "FOREIGN KEY(user_seq) REFERENCES " + TABLE_USER + "(" + COL_USER_SEQ + ")" +
                        ");"
        );

        // TRASH_DISPOSAL (쓰레기 배출) - REQ-007, 010
        db.execSQL(
                "CREATE TABLE TRASH_DISPOSAL (" +
                        "trash_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_seq INTEGER NOT NULL, " +
                        "trash_type TEXT NOT NULL, " +
                        "area TEXT, " +
                        "disposal_date TEXT, " +
                        "is_auto INTEGER DEFAULT 0, " +
                        "dday INTEGER, " +
                        "is_done INTEGER DEFAULT 0, " +
                        "FOREIGN KEY(user_seq) REFERENCES " + TABLE_USER + "(" + COL_USER_SEQ + ")" +
                        ");"
        );

        // TRASH_REGION_INFO (지역별 배출요일 공공데이터) - REQ-010
        // 09.25 : "생활쓰레기배출정보.csv" (전국 공공데이터)를 최초 실행 시 이 테이블에 적재한다.
        // area = 관리구역(읍면동 단위 선택지). 같은 시군구 안에 배출권역이 여러 개인 경우가 있어 3단계로 조회한다.
        db.execSQL(
                "CREATE TABLE TRASH_REGION_INFO (" +
                        "region_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "sido TEXT NOT NULL, " +
                        "sigungu TEXT NOT NULL, " +
                        "area TEXT NOT NULL, " +
                        "trash_type TEXT NOT NULL, " +
                        "disposal_weekday TEXT NOT NULL" +
                        ");"
        );

        // TIP (팁 원본) - REQ-011, 012
        db.execSQL(
                "CREATE TABLE TIP (" +
                        "tip_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "category TEXT NOT NULL, " +
                        "sub_category TEXT, " +
                        "title TEXT NOT NULL, " +
                        "content TEXT" +
                        ");"
        );

        // USER_TIP (내가 쓴 팁) - REQ-013
        db.execSQL(
                "CREATE TABLE USER_TIP (" +
                        "user_tip_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "tip_id INTEGER NOT NULL, " +
                        "user_seq INTEGER NOT NULL, " +
                        "content TEXT NOT NULL, " +
                        "FOREIGN KEY(tip_id) REFERENCES TIP(tip_id), " +
                        "FOREIGN KEY(user_seq) REFERENCES " + TABLE_USER + "(" + COL_USER_SEQ + ")" +
                        ");"
        );

        // ---------- HOME_FAVORITE : 오늘(09.21) 실제 구현 대상 ----------
        db.execSQL(
                "CREATE TABLE " + TABLE_HOME_FAVORITE + " (" +
                        COL_FAV_USER_SEQ + " INTEGER NOT NULL, " +
                        COL_FAV_SOURCE_TYPE + " TEXT NOT NULL, " +
                        COL_FAV_SOURCE_ID + " INTEGER NOT NULL, " +
                        "PRIMARY KEY(" + COL_FAV_USER_SEQ + ", " + COL_FAV_SOURCE_TYPE + ", " + COL_FAV_SOURCE_ID + "), " +
                        "FOREIGN KEY(" + COL_FAV_USER_SEQ + ") REFERENCES " + TABLE_USER + "(" + COL_USER_SEQ + ")" +
                        ");"
        );

        // ---------- TRASH_REGION_INFO 데이터 적재 : 09.25 실제 구현 대상 ----------
        // 공공데이터 CSV(assets/trash_region.csv)를 한 번만 읽어 DB에 넣는다.
        // 매번 onCreate가 호출되는 게 아니라 DB 파일이 없을 때(최초 설치, 혹은 버전업으로 재생성될 때) 딱 한 번만 실행된다.
        TrashRegionLoader.loadFromAssets(this.context, db);

        // ---------- TIP 기본 데이터 적재 : 오늘(09.28) 실제 구현 대상 ----------
        TipSeeder.seed(db);

        Log.d(TAG, "onCreate() 완료 - 테이블 생성과 초기 데이터 적재(쓰레기 배출 공공데이터, 팁)를 마쳤습니다.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "onUpgrade() 호출됨 : DB 버전 " + oldVersion + " -> " + newVersion);

        // v4 -> v5 : SCHEDULE 에 컬럼 하나만 추가되므로 기존 데이터(회원/일정 등)를 지우지 않고 ALTER 로 처리한다.
        if (oldVersion >= 4) {
            if (oldVersion < 5) {
                Log.d(TAG, "v4 -> v5 : SCHEDULE.recur_group_id 컬럼을 ALTER TABLE로 추가합니다 (기존 데이터 유지).");
                db.execSQL("ALTER TABLE SCHEDULE ADD COLUMN recur_group_id INTEGER");
            }
            Log.d(TAG, "onUpgrade() 완료 (데이터 보존 경로).");
            return;
        }

        // v3 이하는 스키마/데이터 구성이 크게 달라 전체 테이블을 지우고 다시 생성한다.
        Log.w(TAG, "v" + oldVersion + "은 마이그레이션 대상이 아니라 전체 테이블을 지우고 다시 만듭니다. 기존 데이터가 모두 삭제됩니다.");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HOME_FAVORITE);
        db.execSQL("DROP TABLE IF EXISTS USER_TIP");
        db.execSQL("DROP TABLE IF EXISTS TIP");
        db.execSQL("DROP TABLE IF EXISTS TRASH_REGION_INFO");
        db.execSQL("DROP TABLE IF EXISTS TRASH_DISPOSAL");
        db.execSQL("DROP TABLE IF EXISTS INGREDIENT");
        db.execSQL("DROP TABLE IF EXISTS UTILITY_BILL");
        db.execSQL("DROP TABLE IF EXISTS CALENDAR_STICKER");
        db.execSQL("DROP TABLE IF EXISTS SCHEDULE_NOTI");
        db.execSQL("DROP TABLE IF EXISTS SCHEDULE");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        onCreate(db);
        Log.d(TAG, "onUpgrade() 완료 (전체 재생성 경로).");
    }
}
