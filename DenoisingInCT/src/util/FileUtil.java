package util;

import java.io.File;

public class FileUtil {

	public static boolean deleteFile(String fileName) {
		File file = new File(fileName);
		if (file.isFile() && file.exists()) {
			file.delete();
			// System.out.println("Delete " + fileName + " successfully.");
			return true;
		} else {
			// System.out.println("Delete " + fileName + " fail.");
			return false;
		}
	}

	public static boolean deleteDirectory(String dir) {

		// add separator if the dir does not have it at the end
		if (!dir.endsWith(File.separator)) {
			dir = dir + File.separator;
		}
		File dirFile = new File(dir);

		// if the dir does not exist or the type is not match
		if (!dirFile.exists() || !dirFile.isDirectory()) {
			System.out.println(dir + " does not exist");
			return false;
		}
		boolean flag = true;

		File[] files = dirFile.listFiles();
		for (int i = 0; i < files.length; i++) {
			// delete a single file
			if (files[i].isFile()) {
				flag = deleteFile(files[i].getAbsolutePath());
				if (!flag) {
					break;
				}
			}
			// delete sub folder
			else {
				flag = deleteDirectory(files[i].getAbsolutePath());
				if (!flag) {
					break;
				}
			}
		}

		if (!flag) {
			System.out.println("fail");
			return false;
		}

		// delete current folder
		if (dirFile.delete()) {
			// System.out.println("Delete "+dir+" successfully.");
			return true;
		} else {
			// System.out.println("Delete "+dir+" fail.");
			return false;
		}
	}
}
