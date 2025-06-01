package sv.edu.ues.fia.controldemedicamentosyarticulosdelhogar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ArticuloMySQLDAO {
    private Context context;
    private WebServiceHelper ws;
    private SQLiteDatabase db;
    public ArticuloMySQLDAO(Context context) {
        this.context = context;
        this.ws = new WebServiceHelper(context);
    }

    public ArticuloMySQLDAO(Context context, SQLiteDatabase db) {
        this.context = context;
        this.db = db;
        this.ws = new WebServiceHelper(context);
    }

    public void sincronizarMySQLConSqlite(Articulo articulo) {

        Cursor cursor = db.query("ARTICULO",
                new String[]{"IDARTICULO"},
                "IDARTICULO = ?",
                new String[]{String.valueOf(articulo.getIdArticulo())},
                null, null, null);

        boolean existe = cursor.moveToFirst();
        cursor.close();

        if (!existe) {
            ContentValues values = new ContentValues();
            values.put("IDARTICULO", articulo.getIdArticulo());
            values.put("IDMARCA", articulo.getIdMarca());
            values.put("IDVIAADMINISTRACION", articulo.getIdViaAdministracion());
            values.put("IDSUBCATEGORIA", articulo.getIdSubCategoria());
            values.put("IDFORMAFARMACEUTICA", articulo.getIdFormaFarmaceutica());
            values.put("NOMBREARTICULO", articulo.getNombreArticulo());
            values.put("DESCRIPCIONARTICULO", articulo.getDescripcionArticulo());
            values.put("RESTRINGIDOARTICULO", articulo.getRestringidoArticulo() ? 1 : 0);
            values.put("PRECIOARTICULO", articulo.getPrecioArticulo());

            db.insert("ARTICULO", null, values);
        }
    }
    //Sincronizar articulo
    public void sincronizarSqliteConMySQL(Articulo articulo) {
        // Primero verificamos si ya existe el artículo en MySQL
        isDuplicate(articulo.getIdArticulo(), existsId -> {
            if (!existsId) {
                // Si no existe, preparamos los parámetros para insertar
                Map<String, String> params = new HashMap<>();
                params.put("idarticulo", String.valueOf(articulo.getIdArticulo()));
                params.put("idmarca", String.valueOf(articulo.getIdMarca()));
                params.put("idviaadministracion", String.valueOf(articulo.getIdViaAdministracion()));
                params.put("idsubcategoria", String.valueOf(articulo.getIdSubCategoria()));
                params.put("idformafarmaceutica", String.valueOf(articulo.getIdFormaFarmaceutica()));
                params.put("nombrearticulo", articulo.getNombreArticulo());
                params.put("descripcionarticulo", articulo.getDescripcionArticulo());
                params.put("restringidoarticulo", articulo.getRestringidoArticulo() ? "1" : "0");
                params.put("precioarticulo", String.valueOf(articulo.getPrecioArticulo()));

                // Realizamos la solicitud POST para insertar el artículo en MySQL
                ws.post("articulo/sincronizar_sqlite_mysql.php", params,
                        response -> {
                        },
                        error -> {
                            Toast.makeText(context, R.string.mysql_sync_error, Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }
    //Actualizar el precio
    public void actualizarPrecioSqlite(SQLiteDatabase db, Runnable onComplete) {
        getAllArticuloMySQL(articulos -> {
            for (Articulo articulo : articulos) {
                ContentValues values = new ContentValues();
                values.put("PRECIOARTICULO", articulo.getPrecioArticulo());

                int rows = db.update("ARTICULO",
                        values,
                        "IDARTICULO = ?",
                        new String[]{String.valueOf(articulo.getIdArticulo())});

                // Opcional: puedes agregar logs para depurar
                Log.d("ActualizarPrecio", "Articulo ID " + articulo.getIdArticulo() + ", filas actualizadas: " + rows);
            }
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }
    //agregar articulo
    public void addArticuloMySQL(Articulo articulo, ArticuloDAO sqliteDAO, Response.Listener<String> callback) {
        tablasSincronizadas(sqliteDAO, iguales -> {
            if (!iguales) {
                Toast.makeText(context, R.string.tables_not_synced, Toast.LENGTH_LONG).show();
                return;
            }

            isDuplicate(articulo.getIdArticulo(), existsId -> {
                if (existsId) {
                    Toast.makeText(context, R.string.duplicate_message, Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, String> params = new HashMap<>();
                params.put("idarticulo", String.valueOf(articulo.getIdArticulo()));
                params.put("idmarca", String.valueOf(articulo.getIdMarca()));
                params.put("idsubcategoria", String.valueOf(articulo.getIdSubCategoria()));
                params.put("nombrearticulo", articulo.getNombreArticulo());
                params.put("descripcionarticulo", articulo.getDescripcionArticulo());
                params.put("restringidoarticulo", articulo.getRestringidoArticulo() ? "1" : "0");

                // Solo enviamos si tienen valor
                if (articulo.getIdViaAdministracion() != null) {
                    params.put("idviaadministracion", String.valueOf(articulo.getIdViaAdministracion()));
                }

                if (articulo.getIdFormaFarmaceutica() != null) {
                    params.put("idformafarmaceutica", String.valueOf(articulo.getIdFormaFarmaceutica()));
                }

                if (articulo.getPrecioArticulo() != null) {
                    params.put("precioarticulo", String.valueOf(articulo.getPrecioArticulo()));
                }

                ws.post("articulo/insertar_articulo.php", params,
                        response -> {
                            Toast.makeText(context, R.string.save_message, Toast.LENGTH_SHORT).show();
                            callback.onResponse(response);
                        },
                        error -> {
                            Toast.makeText(context, R.string.connection_error, Toast.LENGTH_SHORT).show();
                        }
                );
            });
        });
    }

    //Ordener articulo precio mas caro o mas barato
    public void getArticulosOrdenadosPorPrecio(String orden, Response.Listener<List<Articulo>> callback) {
        Map<String, String> params = new HashMap<>();
        params.put("orden", orden);  // el orden que va traer del activity que esta en el spiner

        ws.post("articulo/listar_articulos_ordenados.php", params,
                response -> {
                    try {
                        JSONArray array = new JSONArray(response);
                        List<Articulo> list = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            list.add(new Articulo(
                                    obj.getInt("idarticulo"),
                                    obj.getInt("idmarca"),
                                    obj.has("idviaadministracion") && !obj.isNull("idviaadministracion") ? obj.getInt("idviaadministracion") : 0,
                                    obj.getInt("idsubcategoria"),
                                    obj.has("idformafarmaceutica") && !obj.isNull("idformafarmaceutica") ? obj.getInt("idformafarmaceutica") : 0,
                                    obj.getString("nombrearticulo"),
                                    obj.getString("descripcionarticulo"),
                                    obj.getInt("restringidoarticulo") == 1,
                                    obj.getDouble("precioarticulo")
                            ));
                        }
                        callback.onResponse(list);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.onResponse(Collections.emptyList());
                    }
                },
                error -> {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, R.string.connection_error, Toast.LENGTH_SHORT).show()
                    );
                    callback.onResponse(Collections.emptyList());
                });
    }
    //Obtener articulos
    public void getAllArticuloMySQL(Response.Listener<List<Articulo>> callback) {
        ws.post("articulo/listar_articulos.php", new HashMap<>(),
                response -> {
                    try {
                        JSONArray array = new JSONArray(response);
                        List<Articulo> list = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            list.add(new Articulo(
                                    obj.getInt("idarticulo"),
                                    obj.getInt("idmarca"),
                                    obj.has("idviaadministracion") && !obj.isNull("idviaadministracion") ? obj.getInt("idviaadministracion") : 0,
                                    obj.getInt("idsubcategoria"),
                                    obj.has("idformafarmaceutica") && !obj.isNull("idformafarmaceutica") ? obj.getInt("idformafarmaceutica") : 0,
                                    obj.getString("nombrearticulo"),
                                    obj.getString("descripcionarticulo"),
                                    obj.getInt("restringidoarticulo") == 1,  // Convertir INT a booleano
                                    obj.getDouble("precioarticulo")
                            ));
                        }
                        callback.onResponse(list);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.onResponse(Collections.emptyList());
                    }
                },
                error -> {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, R.string.connection_error, Toast.LENGTH_SHORT).show()
                    );
                    callback.onResponse(Collections.emptyList());
                });
    }
    //Obtener ids de articulos
    public void getAllIdsMySQL(Response.Listener<List<Integer>> callback) {
        ws.post("articulo/listar_articulos.php", new HashMap<>(),
                response -> {
                    try {
                        JSONArray array = new JSONArray(response);
                        List<Integer> ids = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            ids.add(obj.getInt("idarticulo"));  // Solo extraemos los IDs
                        }
                        callback.onResponse(ids);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.onResponse(Collections.emptyList());
                    }
                },
                error -> {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, R.string.connection_error, Toast.LENGTH_SHORT).show()
                    );
                    callback.onResponse(Collections.emptyList());
                });
    }
    //Verificar si las tablas estan sincronizadas
    public void tablasSincronizadas(ArticuloDAO sqliteDAO, Response.Listener<Boolean> callback) {
        getAllIdsMySQL(idsMySQL -> {
            List<Integer> idsSQLite = sqliteDAO.getAllIdsSQLite();

            boolean iguales = idsMySQL.size() == idsSQLite.size() && idsMySQL.containsAll(idsSQLite);

            callback.onResponse(iguales);
        });
    }
    //Actualizar articulo precio
    public void updateArticuloMySQL(Articulo articulo, Response.Listener<String> callback) {
        Map<String, String> params = new HashMap<>();
        params.put("idarticulo", String.valueOf(articulo.getIdArticulo()));
        params.put("idmarca", String.valueOf(articulo.getIdMarca()));
        params.put("idviaadministracion", String.valueOf(articulo.getIdViaAdministracion()));
        params.put("idsubcategoria", String.valueOf(articulo.getIdSubCategoria()));
        params.put("idformafarmaceutica", String.valueOf(articulo.getIdFormaFarmaceutica()));
        params.put("nombrearticulo", articulo.getNombreArticulo());
        params.put("descripcionarticulo", articulo.getDescripcionArticulo());
        params.put("restringidoarticulo", articulo.getRestringidoArticulo() ? "1" : "0");
        params.put("precioarticulo", String.valueOf(articulo.getPrecioArticulo()));

        ws.post("articulo/actualizar_precio.php", params,
                response -> {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, R.string.update_message, Toast.LENGTH_SHORT).show()
                    );
                    callback.onResponse(response);
                },
                error -> {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(context, R.string.connection_error, Toast.LENGTH_SHORT).show();
                        Log.e("ArticuloDAO", "Error en updateArticuloMySQL: " + error.toString());
                    });
                    callback.onResponse(null);
                }
        );
    }
    // Obtener todas las marcas
    public void getAllMarcasMySQL(Response.Listener<List<Marca>> callback) {
        ws.post("articulo/listar_marcas.php", new HashMap<>(),
                response -> {
                    try {
                        JSONArray array = new JSONArray(response);
                        List<Marca> marcas = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            marcas.add(new Marca(
                                    obj.getInt("idmarca"),
                                    obj.getString("nombremarca")
                                    , context
                            ));
                        }
                        callback.onResponse(marcas);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.onResponse(Collections.emptyList());
                    }
                },
                error -> {
                    Toast.makeText(context, R.string.connection_error, Toast.LENGTH_SHORT).show();
                    callback.onResponse(Collections.emptyList());
                });
    }
    // Obtener todas las via Administración
    public void getAllViaAdministracionMySQL(Response.Listener<List<ViaAdministracion>> callback) {
        ws.post("articulo/listar_via_administracion.php", new HashMap<>(),
                response -> {
                    try {
                        JSONArray array = new JSONArray(response);
                        List<ViaAdministracion> viaAdministracions = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            viaAdministracions.add(new ViaAdministracion(
                                    obj.getInt("idviaadministracion"),
                                    obj.getString("tipoadministracion")
                                    , context
                            ));
                        }
                        callback.onResponse(viaAdministracions);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.onResponse(Collections.emptyList());
                    }
                },
                error -> {
                    Toast.makeText(context, R.string.connection_error, Toast.LENGTH_SHORT).show();
                    callback.onResponse(Collections.emptyList());
                });
    }
    // Obtener todas las sub categorias
    public void getAllSubCategoriaMySQL(Response.Listener<List<SubCategoria>> callback) {
        ws.post("articulo/listar_sub_categoria.php", new HashMap<>(),
                response -> {
                    try {
                        JSONArray array = new JSONArray(response);
                        List<SubCategoria> subCategorias = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            subCategorias.add(new SubCategoria(
                                    obj.getInt("idsubcategoria"),
                                    obj.getInt("idcategoria"),
                                    obj.getString("nombresubcategoria")
                            ));
                        }
                        callback.onResponse(subCategorias);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.onResponse(Collections.emptyList());
                    }
                },
                error -> {
                    Toast.makeText(context, R.string.connection_error, Toast.LENGTH_SHORT).show();
                    callback.onResponse(Collections.emptyList());
                });
    }
    // Obtener todas las formas farmaceutica
    public void getAllFormaFarmaceuticaMySQL(Response.Listener<List<FormaFarmaceutica>> callback) {
        ws.post("articulo/listar_forma_farmaceutica.php", new HashMap<>(),
                response -> {
                    try {
                        JSONArray array = new JSONArray(response);
                        List<FormaFarmaceutica> formaFarmaceuticas = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            formaFarmaceuticas.add(new FormaFarmaceutica(
                                    obj.getInt("idformafarmaceutica"),
                                    obj.getString("tipofarmafarmaceutica")
                                    , context
                            ));
                        }
                        callback.onResponse(formaFarmaceuticas);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.onResponse(Collections.emptyList());
                    }
                },
                error -> {
                    Toast.makeText(context, R.string.connection_error, Toast.LENGTH_SHORT).show();
                    callback.onResponse(Collections.emptyList());
                });
    }
    //Eliminar articulo
    public void deleteArticuloMySQL(int idarticulo, Response.Listener<String> callback) {
        Map<String, String> params = new HashMap<>();
        params.put("idarticulo", String.valueOf(idarticulo));

        ws.post("articulo/eliminar_articulo.php", params,
                response -> {
                    Toast.makeText(context, R.string.delete_message, Toast.LENGTH_SHORT).show();
                    callback.onResponse(response);
                },
                e -> Toast.makeText(context, R.string.connection_error, Toast.LENGTH_SHORT).show()
        );
    }
    //Verificar articulo si existe
    private void isDuplicate(int idArticulo, Response.Listener<Boolean> callback) {
        Map<String, String> params = new HashMap<>();
        params.put("idartiuclo", String.valueOf(idArticulo));
        ws.post("articulo/verificar_articulo.php", params,
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
    //Obtener arituclo por id mysql si no encuentra en mysql busca en sqlite
    public void getArticuloMySQLSqlite(int id, Response.Listener<Articulo> callback) {
        // Primero intento obtener desde MySQL
        Map<String, String> params = new HashMap<>();
        params.put("idarticulo", String.valueOf(id));

        ws.post("articulo/obtener_articulo.php", params,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.has("idarticulo")) {
                            Articulo articulo = new Articulo(
                                    obj.getInt("idarticulo"),
                                    obj.getInt("idmarca"),
                                    obj.optInt("idviaadministracion", 0),
                                    obj.getInt("idsubcategoria"),
                                    obj.optInt("idformafarmaceutica", 0),
                                    obj.getString("nombrearticulo"),
                                    obj.getString("descripcionarticulo"),
                                    obj.getInt("restringidoarticulo") == 1,
                                    obj.getDouble("precioarticulo")
                            );
                            // Si encontró en MySQL, devuelve
                            callback.onResponse(articulo);
                        } else {
                            // No existe en MySQL, buscar en SQLite
                            Articulo articuloLocal = getArticuloSQLite(id);
                            callback.onResponse(articuloLocal);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        // Error en JSON, buscar en SQLite
                        Articulo articuloLocal = getArticuloSQLite(id);
                        Toast.makeText(context, "Error en JSON, articulo encontrado en SQLite", Toast.LENGTH_SHORT).show();
                        callback.onResponse(articuloLocal);
                    }
                },
                error -> {
                    // Error de conexión, buscar en SQLite
                    Articulo articuloLocal = getArticuloSQLite(id);
                    callback.onResponse(articuloLocal);
                });
    }

    // Consultar articulo por id en sqlite
    private Articulo getArticuloSQLite(int id) {
        String[] args = {String.valueOf(id)};
        Cursor cursor = db.query("ARTICULO", null, "IDARTICULO = ?", args, null, null, null);
        if (cursor.moveToFirst()) {
            Articulo articulo = new Articulo();
            articulo.setIdArticulo(cursor.getInt(cursor.getColumnIndexOrThrow("IDARTICULO")));
            articulo.setIdMarca(cursor.getInt(cursor.getColumnIndexOrThrow("IDMARCA")));
            articulo.setIdViaAdministracion(cursor.getInt(cursor.getColumnIndexOrThrow("IDVIAADMINISTRACION")));
            articulo.setIdSubCategoria(cursor.getInt(cursor.getColumnIndexOrThrow("IDSUBCATEGORIA")));
            articulo.setIdFormaFarmaceutica(cursor.getInt(cursor.getColumnIndexOrThrow("IDFORMAFARMACEUTICA")));
            articulo.setNombreArticulo(cursor.getString(cursor.getColumnIndexOrThrow("NOMBREARTICULO")));
            articulo.setDescripcionArticulo(cursor.getString(cursor.getColumnIndexOrThrow("DESCRIPCIONARTICULO")));
            articulo.setRestringidoArticulo(cursor.getInt(cursor.getColumnIndexOrThrow("RESTRINGIDOARTICULO")) == 1);
            articulo.setPrecioArticulo(cursor.getDouble(cursor.getColumnIndexOrThrow("PRECIOARTICULO")));
            cursor.close();
            return articulo;
        } else {
            cursor.close();
            return null;
        }
    }

}
