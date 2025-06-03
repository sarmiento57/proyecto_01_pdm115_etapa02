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

public class DetalleExistenciaDAO {

    private SQLiteDatabase conexionDB;
    private Context context;
    private WebServiceHelper ws;


    public DetalleExistenciaDAO(SQLiteDatabase conexionDB, Context context) {
        this.conexionDB = conexionDB;
        this.context = context;
        this.ws = new WebServiceHelper(context);
    }


    public void addExistencia(DetalleExistencia detalleExistencia, CallbackBoolean callback) {
        if (isDuplicate(detalleExistencia.getIdDetalleExistencia())) {
            Toast.makeText(context, R.string.duplicate_message, Toast.LENGTH_SHORT).show();
            callback.onResult(false);
            return;
        } else {
            if (isUsed(detalleExistencia)) {
                Toast.makeText(context, R.string.duplicate_message, Toast.LENGTH_SHORT).show();
                callback.onResult(false);
                return;
            } else {
                ContentValues values = new ContentValues();
                values.put("IDARTICULO", detalleExistencia.getIdArticulo());
                values.put("IDDETALLEEXISTENCIA", detalleExistencia.getIdDetalleExistencia());
                values.put("IDFARMACIA", detalleExistencia.getIdFarmacia());
                values.put("CANTIDADEXISTENCIA", detalleExistencia.getCantidadExistencia());
                values.put("FECHADEVENCIMIENTO", detalleExistencia.getFechaDeVencimiento());

                long insercion = conexionDB.insert("DETALLEEXISTENCIA", null, values);
                if (insercion == -1) {
                    Toast.makeText(context, R.string.existence_insert_error, Toast.LENGTH_SHORT).show();
                    callback.onResult(false);
                } else {
                    // Insert into MySQL
                    Map<String, String> params = new HashMap<>();
                    params.put("idarticulo", String.valueOf(detalleExistencia.getIdArticulo()));
                    params.put("iddetalleexistencia", String.valueOf(detalleExistencia.getIdDetalleExistencia()));
                    params.put("idfarmacia", String.valueOf(detalleExistencia.getIdFarmacia()));
                    params.put("cantidadexistencia", String.valueOf(detalleExistencia.getCantidadExistencia()));
                    params.put("fechadevencimiento", detalleExistencia.getFechaDeVencimiento());

                    ws.post("detalleexistencia/agregar_existencia.php", params,
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
        }
    }


    public void updateExistencia(DetalleExistencia detalleExistencia, CallbackBoolean callback) {
        Map<String, String> params = new HashMap<>();
        params.put("iddetalleexistencia", String.valueOf(detalleExistencia.getIdDetalleExistencia()));
        params.put("cantidadexistencia", String.valueOf(detalleExistencia.getCantidadExistencia()));
        params.put("fechadevencimiento", detalleExistencia.getFechaDeVencimiento());

        ws.post("detalleexistencia/actualizar_existencia.php", params,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.optBoolean("success", false);
                        if (success) {
                            ContentValues values = new ContentValues();
                            values.put("CANTIDADEXISTENCIA", detalleExistencia.getCantidadExistencia());
                            values.put("FECHADEVENCIMIENTO", detalleExistencia.getFechaDeVencimiento());
                            int rowsAffected = conexionDB.update("DETALLEEXISTENCIA", values, "IDDETALLEEXISTENCIA = ?",
                                    new String[]{String.valueOf(detalleExistencia.getIdDetalleExistencia())});
                            if (rowsAffected == 0) {
                                Toast.makeText(context, R.string.not_found_message, Toast.LENGTH_SHORT).show();
                                callback.onResult(false);
                            } else {
                                Toast.makeText(context, R.string.update_message, Toast.LENGTH_SHORT).show();
                                callback.onResult(true);
                            }
                        } else {
                            Toast.makeText(context, R.string.mysql_update_error + json.optString("error"), Toast.LENGTH_LONG).show();
                            callback.onResult(false);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Respuesta JSON inválida", Toast.LENGTH_SHORT).show();
                        callback.onResult(false);
                    }
                },
                error -> {
                    Toast.makeText(context, R.string.mysql_update_connection_error, Toast.LENGTH_LONG).show();
                    callback.onResult(false);
                });
    }


    public List<DetalleExistencia> getAllDetallesExistencia(){
        List<DetalleExistencia> list = new ArrayList<>();
        String sql = "SELECT * FROM DETALLEEXISTENCIA";
        Cursor cursor = conexionDB.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            list.add(new DetalleExistencia(
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDARTICULO")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDDETALLEEXISTENCIA")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDFARMACIA")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("CANTIDADEXISTENCIA")),
                    cursor.getString(cursor.getColumnIndexOrThrow("FECHADEVENCIMIENTO")),
                    context


            ));
        }
        cursor.close();
        return list;
    }


    public List<Articulo> getAllArticulos(){
        List<Articulo> list = new ArrayList<>();
        String sql = "SELECT * FROM ARTICULO";
        Cursor cursor = conexionDB.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            list.add(new Articulo(
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDARTICULO")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDMARCA")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDVIAADMINISTRACION")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDSUBCATEGORIA")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDFORMAFARMACEUTICA")),
                    cursor.getString(cursor.getColumnIndexOrThrow("NOMBREARTICULO")),
                    cursor.getString(cursor.getColumnIndexOrThrow("DESCRIPCIONARTICULO")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("RESTRINGIDOARTICULO")) == 1,
                    cursor.getDouble(cursor.getColumnIndexOrThrow("PRECIOARTICULO"))
            ));
        }
        cursor.close();
        return list;
    }


    public Articulo getArticulo(int id)
    {
        String sql = "SELECT * FROM ARTICULO WHERE IDARTICULO = ?";
        Cursor cursor = conexionDB.rawQuery(sql, new String[]{String.valueOf(id)});
        if (cursor.moveToFirst()) {
            Articulo articulo = new Articulo(
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDARTICULO")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDMARCA")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDVIAADMINISTRACION")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDSUBCATEGORIA")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDFORMAFARMACEUTICA")),
                    cursor.getString(cursor.getColumnIndexOrThrow("NOMBREARTICULO")),
                    cursor.getString(cursor.getColumnIndexOrThrow("DESCRIPCIONARTICULO")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("RESTRINGIDOARTICULO")) == 1,
                    cursor.getDouble(cursor.getColumnIndexOrThrow("PRECIOARTICULO"))
            );
            cursor.close();
            return articulo;
        }
        cursor.close();
        Toast.makeText(context, R.string.not_found_message, Toast.LENGTH_SHORT).show();
        return null;

    }


    private boolean isDuplicate(int id) {
        String sql = "SELECT 1 FROM DETALLEEXISTENCIA WHERE IDDETALLEEXISTENCIA = ?";
        Cursor cursor = conexionDB.rawQuery(sql, new String[]{String.valueOf(id)});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    private boolean isUsed(DetalleExistencia detalleExistencia) {
        String sql = "SELECT 1 FROM DETALLEEXISTENCIA WHERE IDARTICULO = ? AND IDFARMACIA = ?";
        Cursor cursor = conexionDB.rawQuery(sql, new String[]{
                String.valueOf(detalleExistencia.getIdArticulo()),
                String.valueOf(detalleExistencia.getIdFarmacia())
        });
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }



    public List<DetalleExistencia> getAllDetallesExistenciaByIdArticulo(int id){
        List<DetalleExistencia> list = new ArrayList<>();
        String sql = "SELECT * FROM DETALLEEXISTENCIA WHERE IDARTICULO = ?";
        Cursor cursor = conexionDB.rawQuery(sql, new String[]{String.valueOf(id)});
        while (cursor.moveToNext()) {
            list.add(new DetalleExistencia(
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDARTICULO")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDDETALLEEXISTENCIA")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDFARMACIA")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("CANTIDADEXISTENCIA")),
                    cursor.getString(cursor.getColumnIndexOrThrow("FECHADEVENCIMIENTO")),
                    context

            ));
        }
        cursor.close();
        return list;
    }

    public List<DetalleExistencia> getAllDetallesExistenciaByIdFarm(int id){
        List<DetalleExistencia> list = new ArrayList<>();
        String sql = "SELECT * FROM DETALLEEXISTENCIA WHERE IDFARMACIA = ?";
        Cursor cursor = conexionDB.rawQuery(sql, new String[]{String.valueOf(id)});
        while (cursor.moveToNext()) {
            list.add(new DetalleExistencia(
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDARTICULO")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDDETALLEEXISTENCIA")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDFARMACIA")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("CANTIDADEXISTENCIA")),
                    cursor.getString(cursor.getColumnIndexOrThrow("FECHADEVENCIMIENTO")),
                    context


            ));
        }
        cursor.close();
        return list;
    }


    public void deleteExistencia(int id, Runnable onComplete) {
        Map<String, String> params = new HashMap<>();
        params.put("iddetalleexistencia", String.valueOf(id));

        ws.post("detalleexistencia/eliminar_existencia.php", params,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.optBoolean("success", false);
                        if (success) {
                            int rowsAffected = conexionDB.delete("DETALLEEXISTENCIA", "IDDETALLEEXISTENCIA = ?", new String[]{String.valueOf(id)});
                            if (rowsAffected == 0) {
                                Toast.makeText(context, R.string.not_found_message, Toast.LENGTH_SHORT).show();
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

    // contar existencias de sqlite
    private int contarExistenciasSQLite() {
        Cursor cursor = conexionDB.rawQuery("SELECT COUNT(*) FROM DETALLEEXISTENCIA", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    // contar las existencias de mysql
    private void contarExistenciasMySQL(Response.Listener<Integer> callback) {
        ws.post("detalleexistencia/contar_existencias.php", new HashMap<>(),
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

    public void tablasSincronizadas(CallbackBoolean callback) {
        int countSQLite = contarExistenciasSQLite();
        contarExistenciasMySQL(countMySQL -> {
            if (countMySQL == -1) {
                // Error al obtener conteo MySQL
                callback.onResult(false);
            } else {
                callback.onResult(countSQLite == countMySQL);
            }
        });
    }

    // sincronizar existencia
    public void sincronizarExistenciaMysql(DetalleExistencia detalleExistencia) {
        isDuplicateMysql(detalleExistencia.getIdDetalleExistencia(), existsId -> {
            if (!existsId) {
                Map<String, String> params = new HashMap<>();
                params.put("idarticulo", String.valueOf(detalleExistencia.getIdArticulo()));
                params.put("iddetalleexistencia", String.valueOf(detalleExistencia.getIdDetalleExistencia()));
                params.put("idfarmacia", String.valueOf(detalleExistencia.getIdFarmacia()));
                params.put("cantidadexistencia", String.valueOf(detalleExistencia.getCantidadExistencia()));
                params.put("fechadevencimiento", detalleExistencia.getFechaDeVencimiento());
                ws.post("detalleexistencia/sincronizar_sqlite_mysql_existencia.php", params,
                        response -> {
                        },
                        error -> {
                            Toast.makeText(context, R.string.mysql_sync_error, Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    // Verificar si la existencia existe en MySQL
    private void isDuplicateMysql(int idDetalleExistencia, Response.Listener<Boolean> callback) {
        Map<String, String> params = new HashMap<>();
        params.put("iddetalleexistencia", String.valueOf(idDetalleExistencia));
        ws.post("detalleexistencia/verificar_existencia.php", params,
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
