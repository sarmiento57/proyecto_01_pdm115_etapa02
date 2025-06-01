package sv.edu.ues.fia.controldemedicamentosyarticulosdelhogar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.android.volley.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoctorDAO {
    private Context context;
    private WebServiceHelper ws;
    private SQLiteDatabase db;
    public DoctorDAO(Context context) {
        this.context = context;
        this.ws = new WebServiceHelper(context);
    }

    public DoctorDAO(Context context, SQLiteDatabase db) {
        this.context = context;
        this.db = db;
        this.ws = new WebServiceHelper(context);
    }
    // contoar doctores de sqlite
    private int contarDoctoresSQLite() {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM DOCTOR", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
    // contadr los dotores de mysl
    private void contarDoctoresMySQL(Response.Listener<Integer> callback) {
        ws.post("doctor/contar_doctores.php", new HashMap<>(),
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        int count = json.getInt("count");
                        callback.onResponse(count);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.onResponse(-1);
                    }
                },
                error -> {
                    Toast.makeText(context, R.string.mysql_count_error, Toast.LENGTH_SHORT).show();
                    callback.onResponse(-1);
                });
    }
    //metodo para verificar si las tablas estan sincronizadas
    public void tablasSincronizadas(CallbackBoolean callback) {
        int countSQLite = contarDoctoresSQLite();
        contarDoctoresMySQL(countMySQL -> {
            if (countMySQL == -1) {
                // Error al obtener conteo MySQL
                callback.onResult(false);
            } else {
                callback.onResult(countSQLite == countMySQL);
            }
        });
    }
    //sincronizar doctor
    public void sincronizarDoctorMysql(Doctor doctor) {
        // Primero verificamos si ya existe el doctor en MySQL
        isDuplicateMysql(doctor.getIdDoctor(), existsId -> {
            if (!existsId) {
                // Si no existe, preparamos los parámetros para insertar
                Map<String, String> params = new HashMap<>();
                params.put("iddoctor", String.valueOf(doctor.getIdDoctor()));
                params.put("nombredoctor", doctor.getNombreDoctor());
                params.put("especialidaddoctor", doctor.getEspecialidadDoctor());
                params.put("jvpm", doctor.getJvpm());
                // Realizamos la solicitud POST para insertar el doctor en MySQL
                ws.post("doctor/sincronizar_sqlite_mysql.php", params,
                        response -> {
                        },
                        error -> {
                            Toast.makeText(context, R.string.mysql_sync_error, Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }
    //listar todos los doctores de sqlite
    public void getAllDoctorsSQLite(Response.Listener<List<Doctor>> callback) {
        List<Doctor> lista = new ArrayList<>();
        Cursor cursor = db.query("DOCTOR", null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                Doctor d = new Doctor(
                        cursor.getInt(cursor.getColumnIndexOrThrow("IDDOCTOR")),
                        cursor.getString(cursor.getColumnIndexOrThrow("NOMBREDOCTOR")),
                        cursor.getString(cursor.getColumnIndexOrThrow("ESPECIALIDADDOCTOR")),
                        cursor.getString(cursor.getColumnIndexOrThrow("JVPM")),
                        context
                );
                lista.add(d);
            } while (cursor.moveToNext());
        }
        cursor.close();
        callback.onResponse(lista);
    }
    public void addDoctor(Doctor doctor, CallbackBoolean callback) {
        tablasSincronizadas(isSynced -> {
            if (!isSynced) {
                Toast.makeText(context, R.string.tables_not_synced, Toast.LENGTH_LONG).show();
                callback.onResult(false);
                return;
            }
            if (isDuplicate(doctor.getIdDoctor())) {
                Toast.makeText(context, R.string.duplicate_message, Toast.LENGTH_SHORT).show();
                callback.onResult(false);
                return;
            }

            ContentValues values = new ContentValues();
            values.put("IDDOCTOR", doctor.getIdDoctor());
            values.put("NOMBREDOCTOR", doctor.getNombreDoctor());
            values.put("ESPECIALIDADDOCTOR", doctor.getEspecialidadDoctor());
            values.put("JVPM", doctor.getJvpm());

            long result = db.insert("DOCTOR", null, values);
            if (result == -1) {
                Toast.makeText(context, R.string.doctor_insert_error, Toast.LENGTH_SHORT).show();
                callback.onResult(false);
            } else {
                Toast.makeText(context, R.string.save_message, Toast.LENGTH_SHORT).show();
                callback.onResult(true);
            }
        });
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
    //Eliminar doctor en sqlite y mysql
    public void deleteDoctor(int id, Runnable onComplete) {
        Map<String, String> params = new HashMap<>();
        params.put("iddoctor", String.valueOf(id));

        ws.post("doctor/eliminar_doctor.php", params,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.optBoolean("success", false);
                        if (success) {
                            int rows = db.delete("DOCTOR", "IDDOCTOR = ?", new String[]{String.valueOf(id)});
                            if (rows > 0) {
                                Toast.makeText(context, R.string.delete_message, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, R.string.not_found_message, Toast.LENGTH_SHORT).show();
                            }
                            if (onComplete != null) onComplete.run();
                        } else {
                            Toast.makeText(context, R.string.mysql_delete_error + json.optString("error"), Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Respuesta JSON inválida", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(context, R.string.mysql_delete_connection_error, Toast.LENGTH_LONG).show();
                }
        );
    }
    private boolean isDuplicate(int id) {
        Cursor cursor = db.rawQuery("SELECT * FROM DOCTOR WHERE IDDOCTOR = ?", new String[]{String.valueOf(id)});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }
    //Verificar si el doctor existe en MySQL
    private void isDuplicateMysql(int idDoctor, Response.Listener<Boolean> callback) {
        Map<String, String> params = new HashMap<>();
        params.put("iddoctor", String.valueOf(idDoctor));
        ws.post("doctor/verificar_doctor.php", params,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        callback.onResponse(obj.optBoolean("existe", false));
                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onResponse(false);
                    }
                },
                error -> {
                    Toast.makeText(context, R.string.connection_error, Toast.LENGTH_SHORT).show();
                    callback.onResponse(false);
                });
    }
}
