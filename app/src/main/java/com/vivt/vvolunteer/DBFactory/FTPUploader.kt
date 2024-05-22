package com.vivt.vvolunteer.DBFactory

import com.vivt.vvolunteer.BuildConfig
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException


class FTPUploader {

    fun uploadFile(file: File,  remoteDirectoryPath: String) : String? {
        val ftpClient = FTPClient()
        var fileName: String? = null
        try {
            // Подключаемся к FTP-серверу
            ftpClient.connect(BuildConfig.FTP_HOST)
            ftpClient.login(BuildConfig.FTP_USER, BuildConfig.FTP_PASSWORD)
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

            // Открываем файл для передачи
            val fis = FileInputStream(file)
            val bis = BufferedInputStream(fis)

            // Передаем файл на FTP-сервер
            fileName = file.name
            val remoteFilePath = if (remoteDirectoryPath.endsWith("/")) {
                remoteDirectoryPath + fileName
            } else {
                "$remoteDirectoryPath/$fileName"
            }
            ftpClient.storeFile(remoteFilePath, bis)

            // Закрываем потоки и отключаемся от FTP-сервера
            bis.close()
            fis.close()
            ftpClient.logout()
            ftpClient.disconnect()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return fileName
    }
}
