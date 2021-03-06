package silent.kuasapmaterial.libs;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.kuas.ap.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import silent.kuasapmaterial.CourseAlarmService;
import silent.kuasapmaterial.CourseVibrateAlarmService;
import silent.kuasapmaterial.models.CourseModel;

public class VibrateHelper {

	static void setCourseVibrate(Context context, List<List<CourseModel>> courseModelList) {
		if (courseModelList == null) {
			return;
		}

		List<String> keyList = new ArrayList<>();
		List<CourseModel> saveModelList = new ArrayList<>();
		for (int i = 0; i < courseModelList.size(); i++) {
			if (courseModelList.get(i) != null) {
				for (int j = 0; j < courseModelList.get(i).size(); j++) {
					if (courseModelList.get(i).get(j) != null) {
						if (keyList.contains(courseModelList.get(i).get(j).title + i) ||
								(j > 0 && courseModelList.get(i).get(j - 1) != null)) {
							if (!(j == courseModelList.get(i).size() - 1 ||
									courseModelList.get(i).get(j + 1) == null)) {
								continue;
							}
						} else {
							keyList.add(courseModelList.get(i).get(j).title + i);
						}

						CourseModel courseModel = courseModelList.get(i).get(j);
						if (!courseModel.start_time.contains(":") ||
								!courseModel.end_time.contains(":")) {
							courseModel.start_time =
									context.getResources().getStringArray(R.array.start_time)[j];
							courseModel.end_time =
									context.getResources().getStringArray(R.array.start_time)[j];
						}
						courseModel.dayOfWeek = i == 6 ? 1 : (i + 2);
						courseModel.notifyKey = j * 10 + i;
						saveModelList.add(courseModel);
					}
				}
			}
		}

		// Must cancel course alarm if user cancel on web
		List<CourseModel> savedCourseModelList = Utils.loadCourseVibrate(context);
		if (savedCourseModelList != null) {
			for (int i = 0; i < savedCourseModelList.size(); i++) {
				CourseModel courseModel = savedCourseModelList.get(i);
				if (!saveModelList.contains(courseModel)) {
					if (i % 2 == 0) {
						cancelCourseAlarm(context, courseModel.notifyKey * 1000, true);
					} else {
						cancelCourseAlarm(context, courseModel.notifyKey * 10000, false);
					}
				}
			}
		}

		// Must set alarm after cancel
		for (int i = 0; i < saveModelList.size(); i++) {
			CourseModel courseModel = saveModelList.get(i);
			if (i % 2 == 0) {
				setCourseAlarm(context, courseModel.start_time, courseModel.dayOfWeek,
						courseModel.notifyKey * 1000, true);
			} else {
				setCourseAlarm(context, courseModel.end_time, courseModel.dayOfWeek,
						courseModel.notifyKey * 10000, false);
			}
		}

		Utils.saveCourseVibrate(context, saveModelList);
	}

	public static void setCourseVibrate(Context context) {
		List<CourseModel> courseModelList = Utils.loadCourseVibrate(context);
		if (courseModelList != null) {
			for (int i = 0; i < courseModelList.size(); i++) {
				try {
					CourseModel courseModel = courseModelList.get(i);
					if (!courseModel.start_time.contains(":") ||
							!courseModel.end_time.contains(":")) {
						List<String> sectionList = new ArrayList<>(Arrays.asList(
								context.getResources().getStringArray(R.array.course_sections)));
						courseModel.start_time = context.getResources()
								.getStringArray(R.array.start_time)[sectionList
								.indexOf(courseModel.section)];
						courseModel.end_time =
								context.getResources().getStringArray(R.array.end_time)[sectionList
										.indexOf(courseModel.section)];
					}
					if (i % 2 == 0) {
						setCourseAlarm(context, courseModel.start_time, courseModel.dayOfWeek,
								courseModel.notifyKey * 1000, true);
					} else {
						setCourseAlarm(context, courseModel.end_time, courseModel.dayOfWeek,
								courseModel.notifyKey * 10000, false);
					}
				} catch (Exception e) {
					Answers.getInstance().logCustom(
							new CustomEvent("Gson").putCustomAttribute("Type", "Course Vibrate Set")
									.putCustomAttribute("Exception", e.getMessage()));
				}
			}
		}
	}

	public static void cancelCourseAlarm(Context context, int id, boolean isVibrate) {
		Intent intent = new Intent(context, CourseAlarmService.class);

		Bundle bundle = new Bundle();
		bundle.putBoolean("mode", isVibrate);
		intent.putExtras(bundle);

		PendingIntent pendingIntent =
				PendingIntent.getService(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarm = (AlarmManager) context.getSystemService(Service.ALARM_SERVICE);
		alarm.cancel(pendingIntent);
	}

	public static void setCourseAlarm(Context context, String time, int dayOfWeek, int id,
	                                  boolean isVibrate) {
		if (!time.contains(":")) {
			return;
		}

		Intent intent = new Intent(context, CourseVibrateAlarmService.class);

		Bundle bundle = new Bundle();
		bundle.putBoolean("mode", isVibrate);
		intent.putExtras(bundle);

		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
		calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.split(":")[0]));
		calendar.set(Calendar.MINUTE, Integer.parseInt(time.split(":")[1]));
		calendar.set(Calendar.SECOND, 0);
		PendingIntent pendingIntent =
				PendingIntent.getService(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		Date now = new Date(System.currentTimeMillis());
		if (calendar.getTime().before(now)) {
			calendar.add(Calendar.DAY_OF_MONTH, 7);
		}

		AlarmManager alarm = (AlarmManager) context.getSystemService(Service.ALARM_SERVICE);
		alarm.cancel(pendingIntent);
		alarm.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
				AlarmManager.INTERVAL_DAY * 7, pendingIntent);
	}
}
