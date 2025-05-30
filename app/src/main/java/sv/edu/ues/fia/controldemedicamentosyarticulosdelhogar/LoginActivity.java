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
import java.sql.SQLException;

public class LoginActivity extends AppCompatActivity {

    private final ControlBD  controlDB = new ControlBD(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        EditText usernameEditText = findViewById(R.id.login_username);
        EditText passwordEditText = findViewById(R.id.login_password);
        Button loginButton = findViewById(R.id.login_button);

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
}