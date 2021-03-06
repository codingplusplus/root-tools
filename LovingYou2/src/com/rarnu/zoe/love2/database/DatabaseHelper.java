package com.rarnu.zoe.love2.database;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.rarnu.zoe.love2.common.DataInfo;
import com.rarnu.zoe.love2.common.DayInfo;

public class DatabaseHelper {

	private static final String CREATE_TABLE_DAY = "create table love(id int primary key, daystamp text, day int, task int, active int, food int, reading int, news int)";
	private static final String CREATE_TABLE_GROUND = "create table ground(id int primary key, day int, txt text, path text, fav int)";

	private static final String INSERT_DAY = "insert into love (id, daystamp, day, task, active, food, reading, news) values (%d, '%s', %d, %d, %d, %d, %d, %d)";
	private static final String INSERT_GROUND = "insert into ground (id, day, txt, path, fav) values (%d, %d, '%s','%s',0)";

	private static final String UPDATE_DAY = "update love set task=%d where day=%d";
	private static final String UPDATE_GROUND = "update ground set fav=%d where id=%d";

	private SQLiteDatabase db = null;

	public DatabaseHelper(Context context) {
		String dbfn = "/data/data/" + context.getPackageName() + "/data.db";
		File fDb = new File(dbfn);
		if (!fDb.exists()) {
			db = SQLiteDatabase.openOrCreateDatabase(fDb, null);
			db.execSQL(CREATE_TABLE_DAY);
			db.execSQL(CREATE_TABLE_GROUND);
		} else {
			db = SQLiteDatabase.openOrCreateDatabase(fDb, null);
		}
	}

	public void close() {
		db.close();
	}

	public int getDay() {
		int ret = 1;
		Cursor c = db.query("love", new String[] { "day" }, null, null, null,
				null, "id desc", "0,1");
		if (c != null) {
			c.moveToFirst();
			while (!c.isAfterLast()) {
				ret = c.getInt(c.getColumnIndex("day"));
				break;
			}
			c.close();
		}
		int day = generateDay(System.currentTimeMillis());
		if (day != ret) {
			ret = day;
		}
		return ret;
	}

	public void insertDay(long stamp, int task, int active, int food,
			int reading, int news) {
		int id = generateId("love");
		int day = generateDay(stamp);
		String sql = String.format(INSERT_DAY, id, String.valueOf(stamp), day,
				task, active, food, reading, news);
		try {
			db.execSQL(sql);
		} catch (Exception e) {

		}
	}

	public void insertGround(int day, String text, String path) {
		int id = generateId("ground");
		String sql = String.format(INSERT_GROUND, id, day, text, path);
		try {
			db.execSQL(sql);
		} catch (Exception e) {

		}
	}

	public void updateGround(int id, int fav) {
		String sql = String.format(UPDATE_GROUND, fav, id);
		try {
			db.execSQL(sql);
		} catch (Exception e) {

		}
	}

	public void updateDay(int day, int task) {
		String sql = String.format(UPDATE_DAY, task, day);
		try {
			db.execSQL(sql);
		} catch (Exception e) {

		}
	}

	public DayInfo queryDay(int day) {
		DayInfo info = new DayInfo();

		Cursor c = db.query("love", new String[] { "day", "task", "active",
				"food", "reading", "news" }, "day=?",
				new String[] { String.valueOf(day) }, "day", null, "id desc");
		if (c != null) {
			c.moveToFirst();
			while (!c.isAfterLast()) {
				info.day = c.getInt(0);
				info.task = c.getInt(1);
				info.active = c.getInt(2);
				info.food = c.getInt(3);
				info.reading = c.getInt(4);
				info.news = c.getInt(5);
				break;
			}
			c.close();
		}

		return info;
	}

	public List<DataInfo> queryHistory() {
		List<DataInfo> list = new ArrayList<DataInfo>();

		// select day, emotion from love group by day order by id desc;
		Cursor c = db.query("love", new String[] { "day",
				"(task+active+food+reading+news)" }, null, null, "day", null,
				"id asc");
		if (c != null) {
			c.moveToFirst();
			while (!c.isAfterLast()) {
				DataInfo info = new DataInfo();
				info.day = c.getInt(0);
				info.data = c.getInt(1);
				list.add(info);
				c.moveToNext();
			}
			c.close();
		}

		return preDataToData(list);
	}

	public List<DayInfo> queryFullHistory() {
		List<DayInfo> list = new ArrayList<DayInfo>();
		Cursor c = db.query("love", new String[] { "day", "task", "active",
				"food", "reading", "news" }, null, null, "day", null, "id asc");
		if (c != null) {
			c.moveToFirst();
			while (!c.isAfterLast()) {
				DayInfo info = new DayInfo();
				info.day = c.getInt(0);
				info.task = c.getInt(1);
				info.active = c.getInt(2);
				info.food = c.getInt(3);
				info.reading = c.getInt(4);
				info.news = c.getInt(5);
				list.add(info);
				c.moveToNext();
			}
			c.close();
		}
		return list;
	}

	public List<DataInfo> preDataToData(List<DataInfo> list) {
		// refill pre list
		for (int i = 0; i < 21; i++) {
			if ((list.size() - 1) < i) {
				DataInfo info = new DataInfo();
				info.day = i + 1;
				info.data = 99;
				list.add(info);
				continue;
			}
			if (list.get(i).day != i + 1) {
				DataInfo info = new DataInfo();
				info.day = i + 1;
				info.data = 99;
				list.add(i, info);
			}
		}
		return list;
	}

	private int generateId(String table) {
		int ret = 0;
		Cursor c = db.query(table, new String[] { "id" }, null, null, null,
				null, "id desc", "0,1");
		if (c != null) {
			c.moveToFirst();
			while (!c.isAfterLast()) {
				ret = c.getInt(c.getColumnIndex("id")) + 1;
				break;
			}
			c.close();
		}
		return ret;
	}

	private int generateDay(long stamp) {
		Cursor c = db.query("love", new String[] { "daystamp", "day" }, null,
				null, null, null, "id desc", "0,1");
		long time = 0;
		int day = 0;
		if (c != null) {
			c.moveToFirst();
			while (!c.isAfterLast()) {
				day = c.getInt(c.getColumnIndex("day"));
				time = c.getLong(c.getColumnIndex("daystamp"));
				break;
			}
			c.close();
		}
		if (day != 0) {
			Date dLast = new Date(time);
			Date dNow = new Date(stamp);
			Calendar aCalendar = Calendar.getInstance();
			aCalendar.setTime(dLast);
			int day1 = aCalendar.get(Calendar.DAY_OF_YEAR);
			int dayCount = checkLeapYear(aCalendar.get(Calendar.YEAR)) ? 366
					: 365;
			// 365
			aCalendar.setTime(dNow);
			int day2 = aCalendar.get(Calendar.DAY_OF_YEAR);
			// 1
			// -364
			int diffDay = day2 - day1;
			if (diffDay < 0) {
				diffDay = dayCount + diffDay;
			}
			return day + diffDay;
		} else {
			day = 1;
		}
		return day;
	}

	private boolean checkLeapYear(int year) {
		boolean flag = false;
		if ((year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0))) {
			flag = true;
		}
		return flag;
	}
}
