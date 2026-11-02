package com.example.store_manager.du_lieu

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Lop tao va chia se file bao cao CSV.
 *
 * Bao cao ton kho duoc sinh trong thu muc cache cua ung dung, sau do duoc
 * chia se qua FileProvider de nguoi dung mo bang ung dung khac nhu Google
 * Sheets, email hay Drive.
 *
 * Khong can quyen ghi bo nho trong bo nho dung: FileProvider duyet quyen chia
 * se noi dung ma khong can cap quyen luu tru.
 */
class BaoCaoCsv(boiCanh: Context) {
    private val boiCanh = boiCanh.applicationContext

    /**
     * Tao file CSV trong thu muc cache.
     *
     * @param tenFile Ten file, khong can duoi file.
     * @param noiDung Noi dung CSV cua file.
     * @return File da duoc tao.
     */
    fun taoFileCsv(tenFile: String, noiDung: String): File {
        // File duoc dat trong thu muc cache de khong lam cham dung luong,
        // va duoc xoa lai neu may bat buoc.
        val thuMuc = File(boiCanh.cacheDir, THU_MUC_BAO_CAO).apply { mkdirs() }
        val file = File(thuMuc, "$tenFile.csv")

        file.writeText(noiDung, Charsets.UTF_8)

        return file
    }

    /**
     * Lay duong dan chia se cua file qua FileProvider.
     *
     * @param file File can chia se.
     * @return Uri dung de gui sang ung dung nhan.
     */
    fun layUriChiaSe(file: File): Uri {
        // Authority phai khop voi khai bao provider trong AndroidManifest.
        return FileProvider.getUriForFile(
            boiCanh,
            "${boiCanh.packageName}.fileprovider",
            file
        )
    }

    /**
     * Tao Intent chia se file cho cac ung dung khac.
     *
     * @param file      File can chia se.
     * @param tieuDe    Tieu de hien thi tren man hinh chia se.
     * @param loaiMime  Kieu MIME cua file.
     * @return Intent da duoc chuan bi.
     */
    fun taoIntentChiaSe(file: File, tieuDe: String, loaiMime: String): Intent {
        val uri = layUriChiaSe(file)

        return Intent(Intent.ACTION_SEND).apply {
            type = loaiMime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, tieuDe)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    companion object {
        /** Ten thu muc chua file bao cao trong cache. */
        const val THU_MUC_BAO_CAO = "bao_cao"
    }
}
