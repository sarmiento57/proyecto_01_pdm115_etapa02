package sv.edu.ues.fia.controldemedicamentosyarticulosdelhogar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.android.volley.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecetaDAO {
    private SQLiteDatabase db;
    private Context context;
    private WebServiceHelper ws;

    public RecetaDAO(SQLiteDatabase db, Context context) {
        this.db = db;
        this.context = context;
        this.ws = new WebServiceHelper(context);
    }

    public void addReceta(Receta receta, CallbackBoolean callback) {
        if (!existeDoctor(receta.getIdDoctor())) {
            Toast.makeText(context, context.getString(R.string.not_found_message) + context.getString(R.string.id_doctor), Toast.LENGTH_SHORT).show();
            callback.onResult(false);
            return;
        }

        if (!existeCliente(receta.getIdCliente())) {
            Toast.makeText(context, context.getString(R.string.not_found_message) + context.getString(R.string.id_doctor), Toast.LENGTH_SHORT).show();
            callback.onResult(false);
            return;
        }

        if (isDuplicate(receta.getIdReceta())) {
            Toast.makeText(context, R.string.duplicate_message, Toast.LENGTH_SHORT).show();
            callback.onResult(false);
            return;
        }

        ContentValues values = new ContentValues();
        values.put("IDDOCTOR", receta.getIdDoctor());
        values.put("IDCLIENTE", receta.getIdCliente());
        values.put("IDRECETA", receta.getIdReceta());
        values.put("FECHAEXPEDIDA", receta.getFechaExpedida());
        values.put("DESCRIPCION", receta.getDescripcion());

        long insercion = db.insert("RECETA", null, values);
        if (insercion == -1) {
            Toast.makeText(context, R.string.save_error, Toast.LENGTH_SHORT).show();
            callback.onResult(false);
        } else {
            // Insert into MySQL
            Map<String, String> params = new HashMap<>();
            params.put("iddoctor", String.valueOf(receta.getIdDoctor()));
            params.put("idcliente", String.valueOf(receta.getIdCliente()));
            params.put("idreceta", String.valueOf(receta.getIdReceta()));
            params.put("fechaexpedida", receta.getFechaExpedida());
            params.put("descripcion", receta.getDescripcion());

            ws.post("receta/agregar_receta.php", params,
                    response -> {
                        try {
                            JSONObject json = new JSONObject(response);
                            boolean success = json.optBoolean("success", false);
                            if (success) {
                                Toast.makeText(context, R.string.save_message, Toast.LENGTH_SHORT).show();
                                callback.onResult(true);
                            } else {
                                Toast.makeText(context, R.string.mysql_insert_error + json.optString("error"), Toast.LENGTH_LONG).show();
                                callback.onResult(false);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(context, "Respuesta JSON inválida", Toast.LENGTH_SHORT).show();
                            callback.onResult(false);
                        }
                    },
                    error -> {
                        Toast.makeText(context, R.string.mysql_insert_connection_error, Toast.LENGTH_LONG).show();
                        callback.onResult(false);
                    });
        }
    }


    public Receta getReceta(int idReceta) {
        String sql = "SELECT * FROM RECETA WHERE IDRECETA=?";
        Cursor cursor = db.rawQuery(sql, new String[]{
                String.valueOf(idReceta)
        });

        if (cursor.moveToFirst()) {
            Receta receta = new Receta(
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDDOCTOR")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDCLIENTE")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDRECETA")),
                    cursor.getString(cursor.getColumnIndexOrThrow("FECHAEXPEDIDA")),
                    cursor.getString(cursor.getColumnIndexOrThrow("DESCRIPCION")),
                    context
            );
            cursor.close();
            return receta;
        }

        cursor.close();
        Toast.makeText(context, R.string.not_found_message, Toast.LENGTH_SHORT).show();
        return null;
    }

    public List<Receta> getAllRecetas() {
        List<Receta> lista = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM RECETA", null);

        while (cursor.moveToNext()) {
            Receta receta = new Receta(
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDDOCTOR")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDCLIENTE")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDRECETA")),
                    cursor.getString(cursor.getColumnIndexOrThrow("FECHAEXPEDIDA")),
                    cursor.getString(cursor.getColumnIndexOrThrow("DESCRIPCION")),
                    context
            );
            lista.add(receta);
        }
        cursor.close();
        return lista;
    }

    public void updateReceta(Receta receta) {
        ContentValues values = new ContentValues();
        values.put("FECHAEXPEDIDA", receta.getFechaExpedida());
        values.put("DESCRIPCION", receta.getDescripcion());

        int rowsAffected = db.update(
                "RECETA",
                values,
                "IDRECETA=?",
                new String[]{
                        String.valueOf(receta.getIdReceta())
                });

        if (rowsAffected == 0) {
            Toast.makeText(context, context.getString(R.string.not_found_message) + context.getString(R.string.id_receta), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, R.string.update_message, Toast.LENGTH_SHORT).show();
        }
    }

    public void deleteReceta(int idReceta, Runnable onComplete) {
        Map<String, String> params = new HashMap<>();
        params.put("idreceta", String.valueOf(idReceta));

        ws.post("receta/eliminar_receta.php", params,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.optBoolean("success", false);
                        if (success) {
                            int rowsAffected = db.delete(
                                    "RECETA",
                                    "IDRECETA=?",
                                    new String[]{
                                            String.valueOf(idReceta)
                                    });

                            if (rowsAffected == 0) {
                                Toast.makeText(context, context.getString(R.string.not_found_message) + context.getString(R.string.id_receta), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, R.string.delete_message, Toast.LENGTH_SHORT).show();
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

    public List<Doctor> getAllDoctor() {
        List<Doctor> lista = new ArrayList<>();
        String sql = "SELECT * FROM DOCTOR";
        Cursor cursor = db.rawQuery(sql, null);

        if (cursor.moveToFirst()) {
            do {
                int idDoctor = cursor.getInt(cursor.getColumnIndexOrThrow("IDDOCTOR"));
                String nombreDoctor = cursor.getString(cursor.getColumnIndexOrThrow("NOMBREDOCTOR"));

                lista.add(new Doctor(idDoctor, nombreDoctor, context));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return lista;
    }

    public List<Cliente> getAllCliente() {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM CLIENTE";
        Cursor cursor = db.rawQuery(sql, null);

        if (cursor.moveToFirst()) {
            do {
                int idCliente = cursor.getInt(cursor.getColumnIndexOrThrow("IDCLIENTE"));
                String nombreCliente = cursor.getString(cursor.getColumnIndexOrThrow("NOMBRECLIENTE"));

                lista.add(new Cliente(idCliente, nombreCliente, context));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return lista;
    }

    private boolean isDuplicate(int idReceta) {
        String sql = "SELECT 1 FROM RECETA WHERE IDRECETA=?";
        Cursor cursor = db.rawQuery(sql, new String[]{
                String.valueOf(idReceta)
        });

        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }


    private boolean existeDoctor(int idDoctor) {
        Cursor cursor = db.rawQuery("SELECT 1 FROM DOCTOR WHERE IDDOCTOR = ?", new String[]{String.valueOf(idDoctor)});
        boolean existe = cursor.moveToFirst();
        cursor.close();
        return existe;
    }

    private boolean existeCliente(int idCliente) {
        Cursor cursor = db.rawQuery("SELECT 1 FROM CLIENTE WHERE IDCLIENTE = ?", new String[]{String.valueOf(idCliente)});
        boolean existe = cursor.moveToFirst();
        cursor.close();
        return existe;
    }

    // contar recetas de sqlite
    private int contarRecetasSQLite() {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM RECETA", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    // contar las recetas de mysql
    private void contarRecetasMySQL(Response.Listener<Integer> callback) {
        ws.post("receta/contar_recetas.php", new HashMap<>(),
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

    // metodo para verificar si las tablas estan sincronizadas
    public void tablasSincronizadas(CallbackBoolean callback) {
        int countSQLite = contarRecetasSQLite();
        contarRecetasMySQL(countMySQL -> {
            if (countMySQL == -1) {
                callback.onResult(false);
            } else {
                callback.onResult(countSQLite == countMySQL);
            }
        });
    }

    // sincronizar receta
    public void sincronizarRecetaMysql(Receta receta) {
        isDuplicateMysql(receta.getIdReceta(), existsId -> {
            if (!existsId) {
                Map<String, String> params = new HashMap<>();
                params.put("iddoctor", String.valueOf(receta.getIdDoctor()));
                params.put("idcliente", String.valueOf(receta.getIdCliente()));
                params.put("idreceta", String.valueOf(receta.getIdReceta()));
                params.put("fechaexpedida", receta.getFechaExpedida());
                params.put("descripcion", receta.getDescripcion());
                ws.post("receta/sincronizar_sqlite_mysql.php", params,
                        response -> {
                        },
                        error -> {
                            Toast.makeText(context, R.string.mysql_sync_error, Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    // Verificar si la receta existe en MySQL
    private void isDuplicateMysql(int idReceta, Response.Listener<Boolean> callback) {
        Map<String, String> params = new HashMap<>();
        params.put("idreceta", String.valueOf(idReceta));
        ws.post("receta/verificar_receta.php", params,
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

    // Consultar recetas por cliente desde MySQL
    public void getRecetasByClienteMySQL(int idCliente, Response.Listener<List<Receta>> callback) {
        Map<String, String> params = new HashMap<>();
        params.put("idcliente", String.valueOf(idCliente));

        ws.post("receta/consultar_recetas_por_cliente.php", params,
                response -> {
                    List<Receta> recetas = new ArrayList<>();
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.optBoolean("success", false)) {
                            JSONArray jsonRecetas = jsonResponse.optJSONArray("recetas");
                            if (jsonRecetas != null) {
                                for (int i = 0; i < jsonRecetas.length(); i++) {
                                    JSONObject recetaJson = jsonRecetas.getJSONObject(i);
                                    Receta receta = new Receta(
                                            recetaJson.getInt("IDDOCTOR"),
                                            recetaJson.getInt("IDCLIENTE"),
                                            recetaJson.getInt("IDRECETA"),
                                            recetaJson.getString("FECHAEXPEDIDA"),
                                            recetaJson.getString("DESCRIPCION"),
                                            context
                                    );
                                    recetas.add(receta);
                                }
                            }
                            callback.onResponse(recetas);
                        } else {
                            Toast.makeText(context, R.string.mysql_query_error + jsonResponse.optString("error"), Toast.LENGTH_LONG).show();
                            callback.onResponse(new ArrayList<>());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Respuesta JSON inválida", Toast.LENGTH_SHORT).show();
                        callback.onResponse(new ArrayList<>());
                    }
                },
                error -> {
                    Toast.makeText(context, R.string.connection_error, Toast.LENGTH_LONG).show();
                    callback.onResponse(new ArrayList<>());
                });
    }
}
