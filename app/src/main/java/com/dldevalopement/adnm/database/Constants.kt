package com.dldevalopement.adnm.database

/**
 * This file contains constants used throughout the application, primarily for API endpoints and JSON keys.
 * Using `const val` ensures these values are compiled as static constants,
 * making them efficient and accessible without needing a class instance.
 */

// 🌍 Host & Base URL
const val HOST = "https://krim-rachida-recyclage.dz"// The base URL of the local server
//const val HOST = "http://192.168.1.4:8000"
const val URL = "$HOST/api" // The base API endpoint
const val RECAPTCHA_URL = "$URL/verify-recaptcha" // The base URL for recaptcha verification"

const val TERMS_URL = "$HOST/terms"

// ================= Update ==========================================


const val CONFIG_URL = "$HOST/storage/app-settings-android.json"

// ================= Auth (Authentication) Endpoints =================
const val REGISTER_URL = "$URL/register" // Endpoint for user registration
const val LOGIN_URL = "$URL/login" // Endpoint for user login
const val LOGOUT_URL = "$URL/logout" // Endpoint for user logout
const val PROFILE_URL = "$URL/profile" // Endpoint to get user profile
const val DELETE_ACCOUNT_URL = "$URL/user/delete" // Endpoint to delete user account
const val FCM_TOKEN_URL = "$URL/fcm/token" // Endpoint for updating Firebase Cloud Messaging token

// ================= Reporter (المبلِّغ) Endpoints =================
// These endpoints are for users with the 'Reporter' role.
const val REPORTER_ADD_REPORT_URL = "$URL/reports/add" // Endpoint to add a new report
const val REPORTER_MY_REPORTS_URL = "$URL/reports/my" // Endpoint to get a list of the user's own reports
const val REPORTER_SHOW_REPORT_URL = "$URL/reports/show" // + {id} // Endpoint to show a specific report by ID
const val GET_WASTE_TYPES_URL = "$URL/reports/wast-types" // Endpoint to get available waste types

// ================= Collector (الجامع) Endpoints =================
// These endpoints are for users with the 'Collector' role.
const val COLLECTOR_REPORTS_URL = "$URL/collector/reports" // Endpoint to get reports for the collector
const val COLLECTOR_SHOW_REPORT_URL = "$URL/collector/reports/" // + {id} // Endpoint to show a specific report for the collector
const val COLLECTOR_UPDATE_STATUS_URL = "$URL/collector/reports/update-status" // Endpoint to update a report's status
const val COLLECTOR_COLLECT_URL = "$URL/collector/reports/collect" // Endpoint to mark a report as collected
const val COLLECTOR_MAP_REPORTS_URL = "$URL/collector/reports-map" // Endpoint to get reports for the map view

// ================= Admin (المدير) Endpoints =================
// These endpoints are for users with the 'Admin' role.

// 📌 إدارة البلاغات (Report Management)
const val ADMIN_REPORTS_URL = "$URL/admin/reports" // Endpoint to manage all reports
const val ADMIN_UPDATE_REPORT_URL = "$URL/admin/report/update" // Endpoint to update a report by admin
const val ADMIN_DELETE_REPORT_URL = "$URL/admin/report/delete" // Endpoint to delete a report by admin

// 📌 إدارة المستخدمين (User Management)
const val ADMIN_USERS_URL = "$URL/admin/users" // Endpoint to manage all users
const val ADMIN_UPDATE_USER_ROLE_URL = "$URL/admin/user/update-role" // Endpoint to change a user's role
const val ADMIN_UPDATE_USER_URL = "$URL/admin/user/update" // Endpoint to update a user's information
const val ADMIN_TOGGLE_USER_STATUS_URL = "$URL/admin/user/toggle-status" // Endpoint to activate/deactivate a user

// 📌 إدارة أنواع النفايات (Waste Type Management)
const val ADMIN_WASTE_TYPES_URL = "$URL/admin/waste-types" // Endpoint to manage all waste types
const val ADMIN_ADD_WASTE_TYPE_URL = "$URL/admin/waste-type/add" // Endpoint to add a new waste type
const val ADMIN_UPDATE_WASTE_TYPE_URL = "$URL/admin/waste-type/update" // Endpoint to update an existing waste type
const val ADMIN_DELETE_WASTE_TYPE_URL = "$URL/admin/waste-type/delete" // Endpoint to delete a waste type
const val ADMIN_WEEKLY_SUMMARY_URL = "$URL/admin/waste-types/weekly-summary" // Endpoint to get a weekly waste summary

// ================= Keys for JSON Payloads & SharedPreferences =================
// These constants are used to access specific fields within JSON objects
// or as keys for storing data locally (e.g., in SharedPreferences).

// 👤 User Keys
const val USER = "user"
const val NAME = "name"
const val LAST_NAME = "last_name"
const val EMAIL = "email"
const val PASSWORD = "password"
const val USER_TYPE_ID = "user_type_id" // 1=Admin, 2=Collector, 3=Reporter

// 📩 Common Response Keys
const val MESSAGE = "message"
const val SUCCESS = "success"
const val DATA = "data"

// ♻️ Report Keys
const val WASTE_TYPE_ID = "waste_type_id"
const val WEIGHT = "weight"
const val LATITUDE = "latitude"
const val LONGITUDE = "longitude"
const val STATUS = "status"
const val ID = "id"
const val PRICE = "price"
const val TOKEN = "token"
const val AUTHORIZATION = "Authorization"
const val BEARER = "Bearer"
const val TYPE = "type"
const val TYPE_EN = "type_en"
const val TYPE_AR = "type_ar"
const val TYPE_FR = "type_fr"
const val TOTAL_PRICE = "total_price"
const val CREATED_AT = "created_at"
const val ROLE = "role"
const val ACCEPT = "Accept"
const val APPLICATION_JSON = "application/json"
const val REPORT_ID = "report_id"
const val SETTINGS = "settings"
const val IS_FIRST_TIME = "isFirstTime"
const val PHONE_NUMBER = "phone_number"
const val WASTE_TYPES = "waste_types"
const val REPORT = "report"
const val REPORTS = "reports"
