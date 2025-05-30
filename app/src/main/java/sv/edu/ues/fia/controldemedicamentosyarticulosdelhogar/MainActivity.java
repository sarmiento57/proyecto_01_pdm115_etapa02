package sv.edu.ues.fia.controldemedicamentosyarticulosdelhogar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    public static boolean habilitarVerDetalles = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences preferencias = getSharedPreferences("PREFERENCIAS_APP", Context.MODE_PRIVATE);
        boolean primeraVez =  preferencias.getBoolean("primera_vez", true);

        if(primeraVez) {
            SQLiteDatabase db = new DataBaseHelper(this).getWritableDatabase();
            DataBaseHelper DBHelper = new DataBaseHelper(this);
            DBHelper.onCreate(db);
            DBHelper.close();
            SharedPreferences.Editor editor = preferencias.edit();
            editor.putBoolean("primera_vez", false);
            editor.apply();

            WebServiceHelper ws = new WebServiceHelper(this);
            ws.ejecutarScript("dummy.sql");
            Toast.makeText(this, "Base de datos MySQL inicializada", Toast.LENGTH_SHORT).show();
            preferencias.edit().putBoolean("primera_vez", false).apply();
        }
        // Redirigir al LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish(); // Finaliza MainActivity para que no se pueda volver atrás
    }
}