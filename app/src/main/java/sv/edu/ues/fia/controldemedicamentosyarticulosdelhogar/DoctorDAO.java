package sv.edu.ues.fia.controldemedicamentosyarticulosdelhogar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class DoctorDAO {
    private SQLiteDatabase db;
    private Context context;

    public DoctorDAO(SQLiteDatabase db, Context context) {
        this.db = db;
        this.context = context;
    }

    public void addDoctor(Doctor doctor) {
        if (isDuplicate(doctor.getIdDoctor())) {
            Toast.makeText(context, R.string.duplicate_message, Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put("IDDOCTOR", doctor.getIdDoctor());
        values.put("NOMBREDOCTOR", doctor.getNombreDoctor());
        values.put("ESPECIALIDADDOCTOR", doctor.getEspecialidadDoctor());
        values.put("JVPM", doctor.getJvpm());

        db.insert("DOCTOR", null, values);
        Toast.makeText(context, R.string.save_message, Toast.LENGTH_SHORT).show();
    }

    public Doctor getDoctor(int id) {
        Cursor cursor = db.rawQuery("SELECT * FROM DOCTOR WHERE IDDOCTOR = ?", new String[]{String.valueOf(id)});
        if (cursor.moveToFirst()) {
            Doctor doctor = new Doctor(
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDDOCTOR")),
                    cursor.getString(cursor.getColumnIndexOrThrow("NOMBREDOCTOR")),
                    cursor.getString(cursor.getColumnIndexOrThrow("ESPECIALIDADDOCTOR")),
                    cursor.getString(cursor.getColumnIndexOrThrow("JVPM")),
                    context
            );
            cursor.close();
            return doctor;
        }
        cursor.close();
        Toast.makeText(context, R.string.not_found_message, Toast.LENGTH_SHORT).show();
        return null;
    }

    public List<Doctor> getAllDoctors() {
        List<Doctor> list = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM DOCTOR", null);
        while (cursor.moveToNext()) {
            list.add(new Doctor(
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDDOCTOR")),
                    cursor.getString(cursor.getColumnIndexOrThrow("NOMBREDOCTOR")),
                    cursor.getString(cursor.getColumnIndexOrThrow("ESPECIALIDADDOCTOR")),
                    cursor.getString(cursor.getColumnIndexOrThrow("JVPM")),
                    context
            ));
        }
        cursor.close();
        return list;
    }

    public void updateDoctor(Doctor doctor) {
        ContentValues values = new ContentValues();
        values.put("NOMBREDOCTOR", doctor.getNombreDoctor());
        values.put("ESPECIALIDADDOCTOR", doctor.getEspecialidadDoctor());
        values.put("JVPM", doctor.getJvpm());

        int rows = db.update("DOCTOR", values, "IDDOCTOR = ?", new String[]{String.valueOf(doctor.getIdDoctor())});
        Toast.makeText(context, rows > 0 ? R.string.update_message : R.string.not_found_message, Toast.LENGTH_SHORT).show();
    }

    public void deleteDoctor(int id) {
        int rows = db.delete("DOCTOR", "IDDOCTOR = ?", new String[]{String.valueOf(id)});
        Toast.makeText(context, rows > 0 ? R.string.delete_message : R.string.not_found_message, Toast.LENGTH_SHORT).show();
    }

    private boolean isDuplicate(int id) {
        Cursor cursor = db.rawQuery("SELECT * FROM DOCTOR WHERE IDDOCTOR = ?", new String[]{String.valueOf(id)});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }
}
