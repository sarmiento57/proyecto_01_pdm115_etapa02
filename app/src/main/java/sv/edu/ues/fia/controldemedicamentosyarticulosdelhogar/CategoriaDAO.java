package sv.edu.ues.fia.controldemedicamentosyarticulosdelhogar;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.android.volley.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoriaDAO {

    private SQLiteDatabase dbConection;
    private Context context;
    private WebServiceHelper ws;

    public CategoriaDAO(SQLiteDatabase dbConection, Context context) {
        this.dbConection = dbConection;
        this.context = context;
        this.ws = new WebServiceHelper(context);
    }

    //Create

    public void insertarCategoria(Categoria categoria, CallbackBoolean callback) {
        if (isDuplicate(categoria.getIdCategoria())) {
            Toast.makeText(this.context, context.getString(R.string.duplicate_message), Toast.LENGTH_SHORT).show();
            callback.onResult(false);
            return;
        }

        ContentValues category = new ContentValues();
        category.put("IDCATEGORIA", categoria.getIdCategoria());
        category.put("NOMBRECATEGORIA", categoria.getNombreCategoria());

        long insercion = dbConection.insert("CATEGORIA", null, category);
        if (insercion == -1) {
            Toast.makeText(this.context, context.getString(R.string.category_insert_error), Toast.LENGTH_SHORT).show();
            callback.onResult(false);
        } else {
            // Insert into MySQL
            Map<String, String> params = new HashMap<>();
            params.put("idcategoria", String.valueOf(categoria.getIdCategoria()));
            params.put("nombrecategoria", categoria.getNombreCategoria());

            ws.post("categoria/agregar_categoria.php", params,
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

    public ArrayList<Categoria> getAllRows(){
        ArrayList<Categoria> listado = new ArrayList<Categoria>();
        Cursor listadoDB = getDbConection().query("CATEGORIA",null,null,null,null,null,null);
        if (listadoDB.moveToFirst()) {
            listadoDB.moveToFirst();
            for (int i = 0; i < listadoDB.getCount(); i++) {
                Categoria categoria = new Categoria();
                categoria.setIdCategoria(listadoDB.getInt(0));
                categoria.setNombreCategoria(listadoDB.getString(1));
                listado.add(categoria);
                listadoDB.moveToNext();
            }
        }

        return listado;
    }

    public boolean updateCategoria(Categoria categoria){
        String [] id = {Integer.toString(categoria.getIdCategoria())};
        ContentValues cambios = new ContentValues();
        cambios.put("NOMBRECATEGORIA", categoria.getNombreCategoria());
        int control = dbConection.update("CATEGORIA",cambios,"IDCATEGORIA = ?",id);
        if(control == 1){
            return true;
        }else{
            return false;
        }

    }

    public void deleteCategoria(int id, Runnable onComplete) {
        Map<String, String> params = new HashMap<>();
        params.put("idcategoria", String.valueOf(id));

        ws.post("categoria/eliminar_categoria.php", params,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.optBoolean("success", false);
                        if (success) {
                            int rows = dbConection.delete("CATEGORIA", "IDCATEGORIA = ?", new String[]{String.valueOf(id)});
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

    public SQLiteDatabase getDbConection() {
        return dbConection;
    }

    public void setDbConection(SQLiteDatabase dbConection) {
        this.dbConection = dbConection;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Categoria getCategoria(int id) {
        Cursor cursor = getDbConection().rawQuery("SELECT * FROM CATEGORIA WHERE IDCATEGORIA = ?", new String[]{String.valueOf(id)});
        if (cursor.moveToFirst()) {
            Categoria categoria = new Categoria(
                    cursor.getInt(cursor.getColumnIndexOrThrow("IDCATEGORIA")),
                    cursor.getString(cursor.getColumnIndexOrThrow("NOMBRECATEGORIA"))
            );
            cursor.close();
            return categoria;
        }
        cursor.close();
        Toast.makeText(context, R.string.not_found_message, Toast.LENGTH_SHORT).show();
        return null;
    }

    private boolean isDuplicate(int id) {
        Cursor cursor = dbConection.rawQuery("SELECT * FROM CATEGORIA WHERE IDCATEGORIA = ?", new String[]{String.valueOf(id)});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    // contar categorias de sqlite
    private int contarCategoriasSQLite() {
        Cursor cursor = dbConection.rawQuery("SELECT COUNT(*) FROM CATEGORIA", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    // contar las categorias de mysql
    private void contarCategoriasMySQL(Response.Listener<Integer> callback) {
        ws.post("categoria/contar_categorias.php", new HashMap<>(),
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
        int countSQLite = contarCategoriasSQLite();
        contarCategoriasMySQL(countMySQL -> {
            if (countMySQL == -1) {
                // Error al obtener conteo MySQL
                callback.onResult(false);
            } else {
                callback.onResult(countSQLite == countMySQL);
            }
        });
    }

    // sincronizar categoria
    public void sincronizarCategoriaMysql(Categoria categoria) {
        // Primero verificamos si ya existe la categoria en MySQL
        isDuplicateMysql(categoria.getIdCategoria(), existsId -> {
            if (!existsId) {
                // Si no existe, preparamos los parámetros para insertar
                Map<String, String> params = new HashMap<>();
                params.put("idcategoria", String.valueOf(categoria.getIdCategoria()));
                params.put("nombrecategoria", categoria.getNombreCategoria());
                // Realizamos la solicitud POST para insertar la categoria en MySQL
                ws.post("categoria/sincronizar_sqlite_mysql.php", params,
                        response -> {
                        },
                        error -> {
                            Toast.makeText(context, R.string.mysql_sync_error, Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    // Verificar si la categoria existe en MySQL
    private void isDuplicateMysql(int idCategoria, Response.Listener<Boolean> callback) {
        Map<String, String> params = new HashMap<>();
        params.put("idcategoria", String.valueOf(idCategoria));
        ws.post("categoria/verificar_categoria.php", params,
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
