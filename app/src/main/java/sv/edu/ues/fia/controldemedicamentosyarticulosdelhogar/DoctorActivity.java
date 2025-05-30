package sv.edu.ues.fia.controldemedicamentosyarticulosdelhogar;

import android.app.AlertDialog;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;

public class DoctorActivity extends AppCompatActivity {
    private final ValidarAccesoCRUD vac = new ValidarAccesoCRUD(this);
    private DoctorDAO doctorDAO;
    private ArrayAdapter<Doctor> adaptadorDoctores;
    private List<Doctor> listaDoctores;
    private ListView listViewDoctores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor);

        // Initialize DAO with SQLite connection
        SQLiteDatabase conexionDB = new ControlBD(this).getConnection();
        doctorDAO = new DoctorDAO(conexionDB, this);

        TextView txtBusqueda = (TextView) findViewById(R.id.searchDcctor);

        Button btnAgregarDoctor = findViewById(R.id.btnAgregarDoctor);
        btnAgregarDoctor.setVisibility(vac.validarAcceso(1) ? View.VISIBLE : View.INVISIBLE);
        btnAgregarDoctor.setOnClickListener(v -> {showAddDialog();});

        Button btnBuscarDoctor = findViewById(R.id.btnBuscarDoctor);
        btnBuscarDoctor.setVisibility(vac.validarAcceso(2) || vac.validarAcceso(3) || vac.validarAcceso(4)? View.VISIBLE : View.INVISIBLE);
        btnBuscarDoctor.setOnClickListener(v -> {
            try {
                int id = Integer.parseInt(txtBusqueda.getText().toString().trim());
                buscarDoctorPorId(id);
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.invalid_search, Toast.LENGTH_LONG).show();
            }
        });

        listViewDoctores = findViewById(R.id.lvDoctor);
        listViewDoctores.setVisibility(vac.validarAcceso(3) || vac.validarAcceso(4) ? View.VISIBLE : View.INVISIBLE);

        // Fill the ListView
        fillList();

        // Set item click listener for ListView
        listViewDoctores.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Doctor doctor = (Doctor) parent.getItemAtPosition(position);
                showOptionsDialog(doctor);
            }
        });
    }

    private void fillList() {
        listaDoctores = doctorDAO.getAllDoctors();
        adaptadorDoctores = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaDoctores);
        listViewDoctores.setAdapter(adaptadorDoctores);
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_doctor, null);
        builder.setView(dialogView);

        EditText editTextIdDoctor = dialogView.findViewById(R.id.edtIdDoctor);
        EditText editTextNombreDoctor = dialogView.findViewById(R.id.edtNombreDoctor);
        EditText editTextEspecialidadDoctor = dialogView.findViewById(R.id.edtEspecialidadDoctor);
        EditText editTextJvpm = dialogView.findViewById(R.id.edtJVPM);
        Button btnGuardarDoctor = dialogView.findViewById(R.id.btnGuardarDoctor);
        Button btnLimpiarDoctor = dialogView.findViewById(R.id.btnLimpiarDoctor);

        List<View> vistas = Arrays.asList(editTextIdDoctor, editTextNombreDoctor, editTextEspecialidadDoctor, editTextJvpm);
        List<String> listaRegex = Arrays.asList("\\d+", "[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+", "[a-zA-ZáéíóúÁÉÍÓÚñÑ ]++", "[a-zA-ZáéíóúÁÉÍÓÚñÑ ]++");
        List<Integer> mensajesDeError = Arrays.asList(R.string.only_numbers, R.string.only_letters, R.string.only_letters, R.string.only_letters);

        ValidadorDeCampos validadorDeCampos = new ValidadorDeCampos(this, vistas, listaRegex, mensajesDeError);

        final AlertDialog dialog = builder.create();
        btnGuardarDoctor.setOnClickListener(v -> {
            if (validadorDeCampos.validarCampos()) {
                guardarDoctor(editTextIdDoctor, editTextNombreDoctor, editTextEspecialidadDoctor, editTextJvpm);
                dialog.dismiss();
            }
        });
        btnLimpiarDoctor.setOnClickListener(v -> limpiarCampos(editTextIdDoctor, editTextNombreDoctor, editTextEspecialidadDoctor, editTextJvpm));
        dialog.show();
    }

    private void showOptionsDialog(final Doctor doctor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.options);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_options, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.buttonView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(vac.validarAcceso(2))
                    viewDoctor(doctor);
                else
                    Toast.makeText(getApplicationContext(), R.string.action_block, Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });

        dialogView.findViewById(R.id.buttonEdit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(vac.validarAcceso(3))
                    editarDoctor(doctor);
                else
                    Toast.makeText(getApplicationContext(), R.string.action_block, Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });

        dialogView.findViewById(R.id.buttonDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(vac.validarAcceso(4))
                    eliminarDoctor(doctor.getIdDoctor());
                else
                    Toast.makeText(getApplicationContext(), R.string.action_block, Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void guardarDoctor(EditText editTextIdDoctor, EditText editTextNombreDoctor, EditText editTextEspecialidadDoctor, EditText editTextJvpm) {
        int id = Integer.parseInt(editTextIdDoctor.getText().toString());
        String nombre = editTextNombreDoctor.getText().toString().trim();
        String especialidad = editTextEspecialidadDoctor.getText().toString().trim();
        String jvpm = editTextJvpm.getText().toString().trim();

        Doctor doctor = new Doctor(id, nombre, especialidad, jvpm, this);
        doctorDAO.addDoctor(doctor);

        fillList();

        limpiarCampos(editTextIdDoctor, editTextNombreDoctor, editTextEspecialidadDoctor, editTextJvpm);
    }

    private void limpiarCampos(EditText... fields) {
        for (EditText field : fields) {
            field.setText("");
        }
    }

    private void viewDoctor(Doctor doctor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.view);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_doctor, null);

        builder.setView(dialogView);

        EditText editTextIdDoctor = dialogView.findViewById(R.id.edtIdDoctor);

        EditText editTextNombreDoctor = dialogView.findViewById(R.id.edtNombreDoctor);

        EditText editTextEspecialidadDoctor = dialogView.findViewById(R.id.edtEspecialidadDoctor);

        EditText editTextJvpm = dialogView.findViewById(R.id.edtJVPM);

        editTextIdDoctor.setText(String.valueOf(doctor.getIdDoctor()));
        editTextNombreDoctor.setText(doctor.getNombreDoctor());
        editTextEspecialidadDoctor.setText(doctor.getEspecialidadDoctor());
        editTextJvpm.setText(doctor.getJvpm());

        editTextIdDoctor.setEnabled(false);
        editTextNombreDoctor.setEnabled(false);
        editTextEspecialidadDoctor.setEnabled(false);
        editTextJvpm.setEnabled(false);

        // Disable buttons if they exist in the layout
        Button btnGuardarDoctor = dialogView.findViewById(R.id.btnGuardarDoctor);
        Button btnLimpiarDoctor = dialogView.findViewById(R.id.btnLimpiarDoctor);
        if (btnGuardarDoctor != null) {
            btnGuardarDoctor.setVisibility(View.GONE);
        }
        if (btnLimpiarDoctor != null) {
            btnLimpiarDoctor.setVisibility(View.GONE);
        }

        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void editarDoctor(Doctor doctor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.edit);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_doctor, null);
        builder.setView(dialogView);

        EditText editTextIdDoctor = dialogView.findViewById(R.id.edtIdDoctor);
        EditText editTextNombreDoctor = dialogView.findViewById(R.id.edtNombreDoctor);
        EditText editTextEspecialidadDoctor = dialogView.findViewById(R.id.edtEspecialidadDoctor);
        EditText editTextJvpm = dialogView.findViewById(R.id.edtJVPM);

        editTextIdDoctor.setText(String.valueOf(doctor.getIdDoctor()));
        editTextNombreDoctor.setText(doctor.getNombreDoctor());
        editTextEspecialidadDoctor.setText(doctor.getEspecialidadDoctor());
        editTextJvpm.setText(doctor.getJvpm());

        editTextIdDoctor.setEnabled(false);

        Button btnGuardarDoctor = dialogView.findViewById(R.id.btnGuardarDoctor);
        Button btnLimpiarDoctor = dialogView.findViewById(R.id.btnLimpiarDoctor);
        btnLimpiarDoctor.setEnabled(false);

        List<View> vistas = Arrays.asList(editTextNombreDoctor, editTextEspecialidadDoctor, editTextJvpm);
        List<String> listaRegex = Arrays.asList("[a-zA-Z]+", "[a-zA-Z]+", "[a-zA-Z]+");
        List<Integer> mensajesDeError = Arrays.asList(R.string.only_letters, R.string.only_letters, R.string.only_letters);

        ValidadorDeCampos validadorDeCampos = new ValidadorDeCampos(this, vistas, listaRegex, mensajesDeError);

        final AlertDialog dialog = builder.create();
        btnGuardarDoctor.setOnClickListener(v -> {
            if (validadorDeCampos.validarCampos()) {
                doctor.setNombreDoctor(editTextNombreDoctor.getText().toString().trim());
                doctor.setEspecialidadDoctor(editTextEspecialidadDoctor.getText().toString().trim());
                doctor.setJvpm(editTextJvpm.getText().toString().trim());
                doctorDAO.updateDoctor(doctor);
                fillList(); // Refresh the ListView
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void eliminarDoctor(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_delete);
        builder.setMessage(getString(R.string.confirm_delete_message) + ": " + id);
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            doctorDAO.deleteDoctor(id);
            fillList(); // Refresh the ListView
        });
        builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void buscarDoctorPorId(int id) {
        Doctor doctor = doctorDAO.getDoctor(id);
        if (doctor != null) {
            viewDoctor(doctor);
        } else {
            Toast.makeText(this, R.string.not_found_message, Toast.LENGTH_SHORT).show();
        }
    }
}