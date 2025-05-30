package sv.edu.ues.fia.controldemedicamentosyarticulosdelhogar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private final ControlBD  controlDB = new ControlBD(this);
    private WebServiceHelper webServiceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        EditText usernameEditText = findViewById(R.id.login_username);
        EditText passwordEditText = findViewById(R.id.login_password);
        Button loginButton = findViewById(R.id.login_button);
        Button mysqlButton = findViewById(R.id.login_mysql);
        webServiceHelper = new WebServiceHelper(this);

        loginButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (validarUsuario(username, password)) {
                Intent intent = new Intent(LoginActivity.this, MenuActivity.class);
                startActivity(intent);
                Toast.makeText(this, getString(R.string.welcome_message) + " " + username, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, R.string.invalid_credentials, Toast.LENGTH_SHORT).show();
            }
        });

        mysqlButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if(username.isEmpty() || password.isEmpty()){
                Toast.makeText(this, R.string.invalid_credentials, Toast.LENGTH_SHORT).show();
                return;
            }
            validarUsuarioMySQL(username, password);
        });
    }

    private boolean validarUsuario(String username, String password) {
        try {
            SQLiteDatabase conexionDB = controlDB.getConnection();
            Cursor cursor = conexionDB.rawQuery("SELECT * FROM USUARIO WHERE NOMBREUSUARIO = ? AND CLAVE = ?", new String[]{username, password});
            boolean isValid = cursor.getCount() > 0;

            if (isValid) {
                cursor.moveToFirst();
                Log.d("LoginActivity", "UserID: " + cursor.getString(cursor.getColumnIndexOrThrow("IDUSUARIO")));
                // Obtener permisos
                Cursor permisosMenuCursor = conexionDB.rawQuery("SELECT IDOPCION FROM ACCESOUSUARIO WHERE IDUSUARIO = ?"
                        , new String[]{cursor.getString(cursor.getColumnIndexOrThrow("IDUSUARIO"))});
                permisosMenuCursor.moveToFirst();
                SharedPreferences preferencias = getSharedPreferences("PERMISOS_APP", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferencias.edit();
                editor.clear();

                if (permisosMenuCursor.moveToFirst()) {
                    do {
                        String opcion = permisosMenuCursor.getString(permisosMenuCursor.getColumnIndexOrThrow("IDOPCION"));
                        editor.putBoolean(opcion, true);
                        Log.d("LoginActivity", "Permiso guardado: " + opcion);
                    } while (permisosMenuCursor.moveToNext());
                }
                editor.putString("id_usuario",cursor.getString(cursor.getColumnIndexOrThrow("IDUSUARIO")));
                editor.putString("user_name", username);
                editor.apply();
                permisosMenuCursor.close();
            }
            cursor.close();
            conexionDB.close();
            return isValid;
        } catch (SQLiteException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.connection_error, Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private void validarUsuarioMySQL(String username, String password) {
        Map<String, String> params = new HashMap<>();
        params.put("usuario", username);
        params.put("clave", password);

        webServiceHelper.post("login_usuario.php", params,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.getBoolean("success");

                        if (success) {
                            String idUsuario = json.getString("id_usuario");
                            JSONArray permisos = json.getJSONArray("permisos");

                            SharedPreferences prefs = getSharedPreferences("PERMISOS_APP", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.clear();

                            for (int i = 0; i < permisos.length(); i++) {
                                editor.putBoolean(permisos.getString(i), true);
                                Log.d("LoginActivity", "Permiso guardado: " + permisos.getString(i));
                            }

                            editor.putString("id_usuario", idUsuario);
                            editor.putString("user_name", username);
                            editor.apply();

                            Toast.makeText(this, getString(R.string.welcome_message) + " " + username, Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, MenuActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, R.string.invalid_credentials, Toast.LENGTH_SHORT).show();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, R.string.connection_error, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Error de conexión: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
        );
    }
}