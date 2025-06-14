package sv.edu.ues.fia.controldemedicamentosyarticulosdelhogar;

import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Response;
import java.util.Arrays;
import java.util.List;


public class ArticuloMySQLActivity extends AppCompatActivity {
    private ArrayAdapter<Articulo> adapterArticuloMySQ;
    private List<Articulo> listaArticuloMySQ;
    private ListView listViewArticuloMySQ;
    private final ValidarAccesoCRUD vac = new ValidarAccesoCRUD(this);
    private ArticuloMySQLDAO articuloMySQLDAO;  // para web service (MySQL)
    private ArticuloDAO articuloSQLiteDAO;      // para SQLite local
    private SQLiteDatabase conection;
    // Variable global para almacenar el orden de los artículos
    private String orden = "";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_articulo_my_sqlactivity);

        //conexion con las 2 db
        conection = new ControlBD(this).getConnection();
        articuloMySQLDAO = new ArticuloMySQLDAO(this, conection);
        articuloSQLiteDAO = new ArticuloDAO(this, conection);

        listViewArticuloMySQ = findViewById(R.id.itemListvMySQL);
        fillList();

        listViewArticuloMySQ.setOnItemClickListener((parent, view, position, id) -> {
            Articulo articulo = (Articulo) parent.getItemAtPosition(position);
            showOptionsDialog(articulo);
        });
        TextView txtBusqueda = findViewById(R.id.edtSearchItemSqliteMysql);
        //Buscar
        Button btnBuscarArticuloPorId = findViewById(R.id.btnSearchItemMySQL);
        btnBuscarArticuloPorId.setVisibility(vac.validarAcceso(2) || vac.validarAcceso(3) || vac.validarAcceso(4) ? View.VISIBLE : View.INVISIBLE);
        btnBuscarArticuloPorId.setOnClickListener(v -> {
            try {
                int id = Integer.parseInt(txtBusqueda.getText().toString().trim());
                buscarArticulo(id);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.invalid_search, Toast.LENGTH_LONG).show();
            }
        });

        //agregar
        Button btnAddItemMySQL = findViewById(R.id.btnAddItemMySQL);
        btnAddItemMySQL.setVisibility(vac.validarAcceso(1) ? View.VISIBLE : View.INVISIBLE);
        btnAddItemMySQL.setOnClickListener(v -> showAddDialog());
        // sincronizar con sqlite
        Button btnSincronizarSqlite = findViewById(R.id.brnSincronizarSqlite);
        btnSincronizarSqlite.setOnClickListener(v -> {
            articuloMySQLDAO.getAllArticuloMySQL(articulos -> {
                for (Articulo articulo : articulos) {
                    articuloMySQLDAO.sincronizarMySQLConSqlite(articulo);
                }
                Toast.makeText(this, R.string.sync_completed, Toast.LENGTH_SHORT).show();
                conection.close();
            });
        });
        // Configuración del Spinner para ordenar por precio
        Spinner spinnerOrden = findViewById(R.id.spinnerOrdenPrecio);
        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                Arrays.asList(getString(R.string.option_cheaper), getString(R.string.option_expensive)));  // Sólo opciones de más barato y más caro
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrden.setAdapter(adapterSpinner);
        spinnerOrden.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    orden = "asc";  // Más barato (ascendente)
                } else if (position == 1) {
                    orden = "desc";  // Más caro (descendente)
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        // Configuración del botón de búsqueda para mostrar artículo más barato o más caro
        Button btnSearchPriceMySQL = findViewById(R.id.btnSearchPriceMySQL);
        btnSearchPriceMySQL.setOnClickListener(v -> {
            articuloMySQLDAO.getArticulosOrdenadosPorPrecio(orden, articulos -> {
                if (!articulos.isEmpty()) {
                    Articulo articulo = articulos.get(0);
                    Toast.makeText(ArticuloMySQLActivity.this, "Artículo seleccionado: " + articulo.getNombreArticulo(), Toast.LENGTH_SHORT).show();
                    viewArticuloMySQL(articulo);  // Mostrar el artículo en la UI
                } else {
                    Toast.makeText(ArticuloMySQLActivity.this, "No hay artículos disponibles.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    private void fillList() {
        articuloMySQLDAO.getAllArticuloMySQL(articulos -> runOnUiThread(() -> {
            listaArticuloMySQ = articulos;
            adapterArticuloMySQ = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaArticuloMySQ);
            listViewArticuloMySQ.setAdapter(adapterArticuloMySQ);
        }));
    }
    private void showOptionsDialog(final Articulo articulo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.options);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_options, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.buttonView).setOnClickListener(v -> {
            if(vac.validarAcceso(2))
                viewArticuloMySQL(articulo);
            else
                Toast.makeText(getApplicationContext(), R.string.action_block, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.buttonEdit).setOnClickListener(v -> {
            if(vac.validarAcceso(2)){
                dialog.dismiss();
                editArticulo(articulo);
            }
            else{
                Toast.makeText(getApplicationContext(), R.string.action_block, Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });
        dialogView.findViewById(R.id.buttonDelete).setOnClickListener(v -> {
            if(vac.validarAcceso(4))
                deleteArticulo(articulo.getIdArticulo());
            else
                Toast.makeText(getApplicationContext(), R.string.action_block, Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });
        dialog.show();
    }
    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_articulo, null);
        builder.setView(dialogView);

        Spinner spinnerItemMarca = dialogView.findViewById(R.id.spinnerItemMarca);
        Spinner spinnerItemROA = dialogView.findViewById(R.id.spinnerItemROA);
        Spinner spinnerItemSubCategoria = dialogView.findViewById(R.id.spinnerItemSubCategoria);
        Spinner spinnerItemFormaFarmaceutica = dialogView.findViewById(R.id.spinnerItemFormaFarmaceutica);

        EditText idArticulo = dialogView.findViewById(R.id.editTextItemId);
        EditText name = dialogView.findViewById(R.id.editTextItemName);
        EditText description = dialogView.findViewById(R.id.editTextItemDescription);
        CheckBox isRestricted = dialogView.findViewById(R.id.checkBoxItemRestricted);
        EditText price = dialogView.findViewById(R.id.editTextItemPrice);

        Button btnGuardar = dialogView.findViewById(R.id.btnGuardarArticulo);
        Button btnLimpiar = dialogView.findViewById(R.id.btnLimpiarArticulo);

        // Fetch Marcas asynchronously
        articuloMySQLDAO.getAllMarcasMySQL(new Response.Listener<List<Marca>>() {
            @Override
            public void onResponse(List<Marca> marcas) {
                marcas.add(0, new Marca(-1, getString(R.string.select_brand), ArticuloMySQLActivity.this));
                ArrayAdapter<Marca> adapterMarca = new ArrayAdapter<>(ArticuloMySQLActivity.this, android.R.layout.simple_spinner_item, marcas) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getView(position, convertView, parent);
                        Marca marca = getItem(position);
                        if (marca.getIdMarca() == -1) {
                            view.setText(R.string.select_brand);
                        } else {
                            view.setText(marca.getNombreMarca()); // Only show name in the Spinner
                        }
                        return view;
                    }
                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                        Marca marca = getItem(position);
                        if (marca.getIdMarca() == -1) {
                            view.setText(R.string.select_brand);
                        } else {
                            view.setText(marca.getNombreMarca() + " (" + marca.getIdMarca() + ")");
                        }
                        return view;
                    }
                };
                adapterMarca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerItemMarca.setAdapter(adapterMarca);
            }
        });

        // Fetch ViaAdministracion asynchronously
        articuloMySQLDAO.getAllViaAdministracionMySQL(new Response.Listener<List<ViaAdministracion>>() {
            @Override
            public void onResponse(List<ViaAdministracion> viaAdministracions) {
                viaAdministracions.add(0, new ViaAdministracion(-1, getString(R.string.select_admin_route), ArticuloMySQLActivity.this));
                ArrayAdapter<ViaAdministracion> adaptarViaAdministracion = new ArrayAdapter<>(ArticuloMySQLActivity.this, android.R.layout.simple_spinner_item, viaAdministracions) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getView(position, convertView, parent);
                        ViaAdministracion viaAdministracion = getItem(position);
                        if (viaAdministracion.getIdViaAdministracion() == -1) {
                            view.setText(R.string.select_admin_route);
                        } else {
                            view.setText(viaAdministracion.getTipoAdministracion()); // Only show name in the Spinner
                        }
                        return view;
                    }
                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                        ViaAdministracion viaAdministracion = getItem(position);
                        if (viaAdministracion.getIdViaAdministracion() == -1) {
                            view.setText(R.string.select_admin_route);
                        } else {
                            view.setText(viaAdministracion.getTipoAdministracion() + " (" + viaAdministracion.getIdViaAdministracion() + ")");
                        }
                        return view;
                    }
                };
                adaptarViaAdministracion.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerItemROA.setAdapter(adaptarViaAdministracion);
            }
        });

        // Fetch Sub categorias asynchronously
        articuloMySQLDAO.getAllSubCategoriaMySQL(new Response.Listener<List<SubCategoria>>() {
            @Override
            public void onResponse(List<SubCategoria> subCategorias) {
                subCategorias.add(0, new SubCategoria(-1, getString(R.string.select_subcategory), ArticuloMySQLActivity.this));
                ArrayAdapter<SubCategoria> adapterSubCat = new ArrayAdapter<>(ArticuloMySQLActivity.this, android.R.layout.simple_spinner_item, subCategorias) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getView(position, convertView, parent);
                        SubCategoria subCategoria = getItem(position);
                        if (subCategoria.getIdCategoria() == -1) {
                            view.setText(R.string.select_subcategory);
                        } else {
                            view.setText(subCategoria.getNombreSubCategoria()); // Only show name in the Spinner
                        }
                        return view;
                    }
                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                        SubCategoria subCategoria = getItem(position);
                        if (subCategoria.getIdCategoria() == -1) {
                            view.setText(R.string.select_subcategory);
                        } else {
                            view.setText(subCategoria.getNombreSubCategoria() + " (" + subCategoria.getIdCategoria() + ")");
                        }
                        return view;
                    }
                };
                adapterSubCat.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerItemSubCategoria.setAdapter(adapterSubCat);
            }
        });

        // Fetch Sub Forma Farmaceutica asynchronously
        articuloMySQLDAO.getAllFormaFarmaceuticaMySQL(new Response.Listener<List<FormaFarmaceutica>>() {
            @Override
            public void onResponse(List<FormaFarmaceutica> formaFarmaceuticas) {
                formaFarmaceuticas.add(0, new FormaFarmaceutica(-1, getString(R.string.select_pharma_form), ArticuloMySQLActivity.this));
                ArrayAdapter<FormaFarmaceutica> adapterForma = new ArrayAdapter<>(ArticuloMySQLActivity.this, android.R.layout.simple_spinner_item, formaFarmaceuticas) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getView(position, convertView, parent);
                        FormaFarmaceutica formaFarmaceutica = getItem(position);
                        if (formaFarmaceutica.getIdFormaFarmaceutica() == -1) {
                            view.setText(getString(R.string.select_pharma_form));
                        } else {
                            view.setText(formaFarmaceutica.getTipoFormaFarmaceutica()); // Only show name in the Spinner
                        }
                        return view;
                    }
                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                        FormaFarmaceutica formaFarmaceutica = getItem(position);
                        if (formaFarmaceutica.getIdFormaFarmaceutica() == -1) {
                            view.setText(getString(R.string.select_pharma_form));
                        } else {
                            view.setText(formaFarmaceutica.getTipoFormaFarmaceutica() + " (" + formaFarmaceutica.getIdFormaFarmaceutica() + ")");
                        }
                        return view;
                    }
                };
                adapterForma.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerItemFormaFarmaceutica.setAdapter(adapterForma);
            }
        });

        AlertDialog dialog = builder.create();

        btnLimpiar.setOnClickListener(v -> {
            spinnerItemMarca.setSelection(0);
            spinnerItemROA.setSelection(0);
            spinnerItemSubCategoria.setSelection(0);
            spinnerItemFormaFarmaceutica.setSelection(0);
            idArticulo.setText("");
            name.setText("");
            description.setText("");
            isRestricted.setChecked(false);
            price.setText("");
        });

        btnGuardar.setOnClickListener(v -> {
            boolean hayError = false;

            String nombre = name.getText().toString().trim();
            String descripcion = description.getText().toString().trim();
            String precioTexto = price.getText().toString().trim();

            if (nombre.isEmpty()) {
                name.setError(getString(R.string.field_empty));
                hayError = true;
            }

            if (descripcion.isEmpty()) {
                description.setError(getString(R.string.field_empty));
                hayError = true;
            }

            if (precioTexto.isEmpty()) {
                price.setError(getString(R.string.field_empty));
                hayError = true;
            }

            Marca marcaSeleccionada = (Marca) spinnerItemMarca.getSelectedItem();
            SubCategoria subSeleccionada = (SubCategoria) spinnerItemSubCategoria.getSelectedItem();
            ViaAdministracion viaSeleccionada = (ViaAdministracion) spinnerItemROA.getSelectedItem();
            FormaFarmaceutica formaSeleccionada = (FormaFarmaceutica) spinnerItemFormaFarmaceutica.getSelectedItem();

            if (marcaSeleccionada.getIdMarca() == -1) {
                View view = spinnerItemMarca.getSelectedView();
                if (view instanceof TextView) {
                    ((TextView) view).setError("");
                    ((TextView) view).setTextColor(Color.RED);
                }
                hayError = true;
            }

            if (subSeleccionada.getIdSubCategoria() == -1) {
                View view = spinnerItemSubCategoria.getSelectedView();
                if (view instanceof TextView) {
                    ((TextView) view).setError("");
                    ((TextView) view).setTextColor(Color.RED);
                }
                hayError = true;
            }

            if (hayError) {
                Toast.makeText(this, getString(R.string.field_empty), Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int id = Integer.parseInt(idArticulo.getText().toString());
                int brand = marcaSeleccionada.getIdMarca();
                int subCat = subSeleccionada.getIdSubCategoria();
                Integer ROA = (viaSeleccionada.getIdViaAdministracion() == -1) ? null : viaSeleccionada.getIdViaAdministracion();
                Integer PF = (formaSeleccionada.getIdFormaFarmaceutica() == -1) ? null : formaSeleccionada.getIdFormaFarmaceutica();
                boolean restringido = isRestricted.isChecked();
                double precio = Double.parseDouble(precioTexto);

                Articulo art = new Articulo(id, brand, ROA, subCat, PF, nombre, descripcion, restringido, precio);
                articuloMySQLDAO.addArticuloMySQL(art, articuloSQLiteDAO, response -> {
                    fillList();
                    dialog.dismiss();
                });

            } catch (NumberFormatException e) {
                Toast.makeText(this, getString(R.string.only_numbers), Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void editArticulo(Articulo articulo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.edit);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_articulo, null);
        builder.setView(dialogView);

        Spinner spinnerItemMarca = dialogView.findViewById(R.id.spinnerItemMarca);
        Spinner spinnerItemROA = dialogView.findViewById(R.id.spinnerItemROA);
        Spinner spinnerItemSubCategoria = dialogView.findViewById(R.id.spinnerItemSubCategoria);
        Spinner spinnerItemFormaFarmaceutica = dialogView.findViewById(R.id.spinnerItemFormaFarmaceutica);

        EditText idArticulo = dialogView.findViewById(R.id.editTextItemId);
        EditText name = dialogView.findViewById(R.id.editTextItemName);
        EditText description = dialogView.findViewById(R.id.editTextItemDescription);
        CheckBox isRestricted = dialogView.findViewById(R.id.checkBoxItemRestricted);
        EditText price = dialogView.findViewById(R.id.editTextItemPrice);

        spinnerItemMarca.setEnabled(false);  // Hacer el spinner de Farmacia no editable
        spinnerItemROA.setEnabled(false); // Hacer el spinner de Proveedor no editable
        spinnerItemSubCategoria.setEnabled(false);
        spinnerItemFormaFarmaceutica.setEnabled(false);
        idArticulo.setEnabled(false);
        name.setEnabled(false);
        description.setEnabled(false);
        isRestricted.setEnabled(false);

// Fetch Marcas asynchronously
        articuloMySQLDAO.getAllMarcasMySQL(new Response.Listener<List<Marca>>() {
            @Override
            public void onResponse(List<Marca> marcas) {
                marcas.add(0, new Marca(-1, getString(R.string.select_brand), ArticuloMySQLActivity.this));
                ArrayAdapter<Marca> adapterMarca = new ArrayAdapter<>(ArticuloMySQLActivity.this, android.R.layout.simple_spinner_item, marcas) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getView(position, convertView, parent);
                        Marca marca = getItem(position);
                        if (marca.getIdMarca() == -1) {
                            view.setText(R.string.select_brand);
                        } else {
                            view.setText(marca.getNombreMarca()); // Only show name in the Spinner
                        }
                        return view;
                    }
                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                        Marca marca = getItem(position);
                        if (marca.getIdMarca() == -1) {
                            view.setText(R.string.select_brand);
                        } else {
                            view.setText(marca.getNombreMarca() + " (" + marca.getIdMarca() + ")");
                        }
                        return view;
                    }
                };
                adapterMarca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerItemMarca.setAdapter(adapterMarca);

                // Set selected item in the spinner
                for (int i = 0; i < marcas.size(); i++) {
                    if (marcas.get(i).getIdMarca() == articulo.getIdMarca()) {
                        spinnerItemMarca.setSelection(i);
                        break;
                    }
                }
            }
        });

        // Fetch ViaAdministracion asynchronously
        articuloMySQLDAO.getAllViaAdministracionMySQL(new Response.Listener<List<ViaAdministracion>>() {
            @Override
            public void onResponse(List<ViaAdministracion> viaAdministracions) {
                viaAdministracions.add(0, new ViaAdministracion(-1, getString(R.string.select_admin_route), ArticuloMySQLActivity.this));
                ArrayAdapter<ViaAdministracion> adaptarViaAdministracion = new ArrayAdapter<>(ArticuloMySQLActivity.this, android.R.layout.simple_spinner_item, viaAdministracions) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getView(position, convertView, parent);
                        ViaAdministracion viaAdministracion = getItem(position);
                        if (viaAdministracion.getIdViaAdministracion() == -1) {
                            view.setText(R.string.select_admin_route);
                        } else {
                            view.setText(viaAdministracion.getTipoAdministracion()); // Only show name in the Spinner
                        }
                        return view;
                    }
                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                        ViaAdministracion viaAdministracion = getItem(position);
                        if (viaAdministracion.getIdViaAdministracion() == -1) {
                            view.setText(R.string.select_admin_route);
                        } else {
                            view.setText(viaAdministracion.getTipoAdministracion() + " (" + viaAdministracion.getIdViaAdministracion() + ")");
                        }
                        return view;
                    }
                };
                adaptarViaAdministracion.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerItemROA.setAdapter(adaptarViaAdministracion);

                // Set selected item in the spinner
                for (int i = 0; i < viaAdministracions.size(); i++) {
                    if (viaAdministracions.get(i).getIdViaAdministracion() == articulo.getIdViaAdministracion()) {
                        spinnerItemROA.setSelection(i);
                        break;
                    }
                }
            }
        });

        // Fetch Sub categorias asynchronously
        articuloMySQLDAO.getAllSubCategoriaMySQL(new Response.Listener<List<SubCategoria>>() {
            @Override
            public void onResponse(List<SubCategoria> subCategorias) {
                subCategorias.add(0, new SubCategoria(-1, getString(R.string.select_subcategory), ArticuloMySQLActivity.this));
                ArrayAdapter<SubCategoria> adapterSubCat = new ArrayAdapter<>(ArticuloMySQLActivity.this, android.R.layout.simple_spinner_item, subCategorias) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getView(position, convertView, parent);
                        SubCategoria subCategoria = getItem(position);
                        if (subCategoria.getIdCategoria() == -1) {
                            view.setText(R.string.select_subcategory);
                        } else {
                            view.setText(subCategoria.getNombreSubCategoria()); // Only show name in the Spinner
                        }
                        return view;
                    }
                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                        SubCategoria subCategoria = getItem(position);
                        if (subCategoria.getIdCategoria() == -1) {
                            view.setText(R.string.select_subcategory);
                        } else {
                            view.setText(subCategoria.getNombreSubCategoria() + " (" + subCategoria.getIdCategoria() + ")");
                        }
                        return view;
                    }
                };
                adapterSubCat.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerItemSubCategoria.setAdapter(adapterSubCat);

                // Set selected item in the spinner
                for (int i = 0; i < subCategorias.size(); i++) {
                    if (subCategorias.get(i).getIdSubCategoria() == articulo.getIdSubCategoria()) {
                        spinnerItemSubCategoria.setSelection(i);
                        break;
                    }
                }
            }
        });

        // Fetch Sub Forma Farmaceutica asynchronously
        articuloMySQLDAO.getAllFormaFarmaceuticaMySQL(new Response.Listener<List<FormaFarmaceutica>>() {
            @Override
            public void onResponse(List<FormaFarmaceutica> formaFarmaceuticas) {
                formaFarmaceuticas.add(0, new FormaFarmaceutica(-1, getString(R.string.select_pharma_form), ArticuloMySQLActivity.this));
                ArrayAdapter<FormaFarmaceutica> adapterForma = new ArrayAdapter<>(ArticuloMySQLActivity.this, android.R.layout.simple_spinner_item, formaFarmaceuticas) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getView(position, convertView, parent);
                        FormaFarmaceutica formaFarmaceutica = getItem(position);
                        if (formaFarmaceutica.getIdFormaFarmaceutica() == -1) {
                            view.setText(getString(R.string.select_pharma_form));
                        } else {
                            view.setText(formaFarmaceutica.getTipoFormaFarmaceutica()); // Only show name in the Spinner
                        }
                        return view;
                    }
                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                        FormaFarmaceutica formaFarmaceutica = getItem(position);
                        if (formaFarmaceutica.getIdFormaFarmaceutica() == -1) {
                            view.setText(getString(R.string.select_pharma_form));
                        } else {
                            view.setText(formaFarmaceutica.getTipoFormaFarmaceutica() + " (" + formaFarmaceutica.getIdFormaFarmaceutica() + ")");
                        }
                        return view;
                    }
                };
                adapterForma.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerItemFormaFarmaceutica.setAdapter(adapterForma);

                // Set selected item in the spinner
                for (int i = 0; i < formaFarmaceuticas.size(); i++) {
                    if (formaFarmaceuticas.get(i).getIdFormaFarmaceutica() == articulo.getIdFormaFarmaceutica()) {
                        spinnerItemFormaFarmaceutica.setSelection(i);
                        break;
                    }
                }
            }
        });

        // Populate EditTexts with current DetalleCompra data
        idArticulo.setText(String.valueOf(articulo.getIdArticulo()));
        name.setText(articulo.getNombreArticulo());
        description.setText(articulo.getDescripcionArticulo());
        isRestricted.setChecked(articulo.getRestringidoArticulo());
        price.setText(String.valueOf(articulo.getPrecioArticulo()));

        Button btnGuardarArticulo = dialogView.findViewById(R.id.btnGuardarArticulo);


        // Validación de campos
        List<View> vistas = Arrays.asList(price);
        List<String> listaRegex = Arrays.asList("^[0-9]+(\\.[0-9]{1,2})?$");
        List<Integer> mensajesDeError = Arrays.asList(R.string.only_numbers);
        ValidadorDeCampos validadorDeCampos = new ValidadorDeCampos(this, vistas, listaRegex, mensajesDeError);


        final AlertDialog dialog = builder.create();

        btnGuardarArticulo.setOnClickListener(v -> {
            if (validadorDeCampos.validarCampos()) {

                double nuevoPrecio = Double.parseDouble(price.getText().toString());

                articulo.setPrecioArticulo(nuevoPrecio);

                articuloMySQLDAO.updateArticuloMySQL(articulo, response -> {
                    fillList();
                    dialog.dismiss();
                });
            }
        });

        dialog.show();
    }
    //eliminar articulo de mysql
    private void deleteArticulo(int idarticulo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_delete);
        builder.setMessage(getString(R.string.confirm_delete_message) + ": " + idarticulo);

        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            articuloMySQLDAO.deleteArticuloMySQL(idarticulo, response -> {
                fillList();
            });
            dialog.dismiss();
        });

        builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    //ver articulo de mysql
    private void viewArticuloMySQL(Articulo articulo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.view);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_articulo, null);
        builder.setView(dialogView);

        Spinner spinnerItemMarca = dialogView.findViewById(R.id.spinnerItemMarca);
        Spinner spinnerItemROA = dialogView.findViewById(R.id.spinnerItemROA);
        Spinner spinnerItemSubCategoria = dialogView.findViewById(R.id.spinnerItemSubCategoria);
        Spinner spinnerItemFormaFarmaceutica = dialogView.findViewById(R.id.spinnerItemFormaFarmaceutica);

        EditText idArticulo = dialogView.findViewById(R.id.editTextItemId);
        EditText name = dialogView.findViewById(R.id.editTextItemName);
        EditText description = dialogView.findViewById(R.id.editTextItemDescription);
        CheckBox isRestricted = dialogView.findViewById(R.id.checkBoxItemRestricted);
        EditText price = dialogView.findViewById(R.id.editTextItemPrice);

        spinnerItemMarca.setEnabled(false);  // Hacer el spinner de Farmacia no editable
        spinnerItemROA.setEnabled(false); // Hacer el spinner de Proveedor no editable
        spinnerItemSubCategoria.setEnabled(false);
        spinnerItemFormaFarmaceutica.setEnabled(false);
        idArticulo.setEnabled(false);
        name.setEnabled(false);
        description.setEnabled(false);
        isRestricted.setEnabled(false);
        price.setEnabled(false);

        // Deshabilitar los botones del diálogo
        Button btnGuardar = dialogView.findViewById(R.id.btnGuardarArticulo);
        Button btnLimpiar = dialogView.findViewById(R.id.btnLimpiarArticulo);

        if (btnGuardar != null) {
            btnGuardar.setVisibility(View.GONE);
        }
        if (btnLimpiar != null) {
            btnLimpiar.setVisibility(View.GONE);
        }

// Fetch Marcas asynchronously
        articuloMySQLDAO.getAllMarcasMySQL(new Response.Listener<List<Marca>>() {
            @Override
            public void onResponse(List<Marca> marcas) {
                marcas.add(0, new Marca(-1, getString(R.string.select_brand), ArticuloMySQLActivity.this));
                ArrayAdapter<Marca> adapterMarca = new ArrayAdapter<>(ArticuloMySQLActivity.this, android.R.layout.simple_spinner_item, marcas) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getView(position, convertView, parent);
                        Marca marca = getItem(position);
                        if (marca.getIdMarca() == -1) {
                            view.setText(R.string.select_brand);
                        } else {
                            view.setText(marca.getNombreMarca()); // Only show name in the Spinner
                        }
                        return view;
                    }
                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                        Marca marca = getItem(position);
                        if (marca.getIdMarca() == -1) {
                            view.setText(R.string.select_brand);
                        } else {
                            view.setText(marca.getNombreMarca() + " (" + marca.getIdMarca() + ")");
                        }
                        return view;
                    }
                };
                adapterMarca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerItemMarca.setAdapter(adapterMarca);

                // Set selected item in the spinner
                for (int i = 0; i < marcas.size(); i++) {
                    if (marcas.get(i).getIdMarca() == articulo.getIdMarca()) {
                        spinnerItemMarca.setSelection(i);
                        break;
                    }
                }
            }
        });

        // Fetch ViaAdministracion asynchronously
        articuloMySQLDAO.getAllViaAdministracionMySQL(new Response.Listener<List<ViaAdministracion>>() {
            @Override
            public void onResponse(List<ViaAdministracion> viaAdministracions) {
                viaAdministracions.add(0, new ViaAdministracion(-1, getString(R.string.select_admin_route), ArticuloMySQLActivity.this));
                ArrayAdapter<ViaAdministracion> adaptarViaAdministracion = new ArrayAdapter<>(ArticuloMySQLActivity.this, android.R.layout.simple_spinner_item, viaAdministracions) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getView(position, convertView, parent);
                        ViaAdministracion viaAdministracion = getItem(position);
                        if (viaAdministracion.getIdViaAdministracion() == -1) {
                            view.setText(R.string.select_admin_route);
                        } else {
                            view.setText(viaAdministracion.getTipoAdministracion()); // Only show name in the Spinner
                        }
                        return view;
                    }
                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                        ViaAdministracion viaAdministracion = getItem(position);
                        if (viaAdministracion.getIdViaAdministracion() == -1) {
                            view.setText(R.string.select_admin_route);
                        } else {
                            view.setText(viaAdministracion.getTipoAdministracion() + " (" + viaAdministracion.getIdViaAdministracion() + ")");
                        }
                        return view;
                    }
                };
                adaptarViaAdministracion.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerItemROA.setAdapter(adaptarViaAdministracion);

                // Set selected item in the spinner
                for (int i = 0; i < viaAdministracions.size(); i++) {
                    if (viaAdministracions.get(i).getIdViaAdministracion() == articulo.getIdViaAdministracion()) {
                        spinnerItemROA.setSelection(i);
                        break;
                    }
                }
            }
        });

        // Fetch Sub categorias asynchronously
        articuloMySQLDAO.getAllSubCategoriaMySQL(new Response.Listener<List<SubCategoria>>() {
            @Override
            public void onResponse(List<SubCategoria> subCategorias) {
                subCategorias.add(0, new SubCategoria(-1, getString(R.string.select_subcategory), ArticuloMySQLActivity.this));
                ArrayAdapter<SubCategoria> adapterSubCat = new ArrayAdapter<>(ArticuloMySQLActivity.this, android.R.layout.simple_spinner_item, subCategorias) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getView(position, convertView, parent);
                        SubCategoria subCategoria = getItem(position);
                        if (subCategoria.getIdCategoria() == -1) {
                            view.setText(R.string.select_subcategory);
                        } else {
                            view.setText(subCategoria.getNombreSubCategoria()); // Only show name in the Spinner
                        }
                        return view;
                    }
                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                        SubCategoria subCategoria = getItem(position);
                        if (subCategoria.getIdCategoria() == -1) {
                            view.setText(R.string.select_subcategory);
                        } else {
                            view.setText(subCategoria.getNombreSubCategoria() + " (" + subCategoria.getIdCategoria() + ")");
                        }
                        return view;
                    }
                };
                adapterSubCat.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerItemSubCategoria.setAdapter(adapterSubCat);

                // Set selected item in the spinner
                for (int i = 0; i < subCategorias.size(); i++) {
                    if (subCategorias.get(i).getIdSubCategoria() == articulo.getIdSubCategoria()) {
                        spinnerItemSubCategoria.setSelection(i);
                        break;
                    }
                }
            }
        });

        // Fetch Sub Forma Farmaceutica asynchronously
        articuloMySQLDAO.getAllFormaFarmaceuticaMySQL(new Response.Listener<List<FormaFarmaceutica>>() {
            @Override
            public void onResponse(List<FormaFarmaceutica> formaFarmaceuticas) {
                formaFarmaceuticas.add(0, new FormaFarmaceutica(-1, getString(R.string.select_pharma_form), ArticuloMySQLActivity.this));
                ArrayAdapter<FormaFarmaceutica> adapterForma = new ArrayAdapter<>(ArticuloMySQLActivity.this, android.R.layout.simple_spinner_item, formaFarmaceuticas) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getView(position, convertView, parent);
                        FormaFarmaceutica formaFarmaceutica = getItem(position);
                        if (formaFarmaceutica.getIdFormaFarmaceutica() == -1) {
                            view.setText(getString(R.string.select_pharma_form));
                        } else {
                            view.setText(formaFarmaceutica.getTipoFormaFarmaceutica()); // Only show name in the Spinner
                        }
                        return view;
                    }
                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                        FormaFarmaceutica formaFarmaceutica = getItem(position);
                        if (formaFarmaceutica.getIdFormaFarmaceutica() == -1) {
                            view.setText(getString(R.string.select_pharma_form));
                        } else {
                            view.setText(formaFarmaceutica.getTipoFormaFarmaceutica() + " (" + formaFarmaceutica.getIdFormaFarmaceutica() + ")");
                        }
                        return view;
                    }
                };
                adapterForma.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerItemFormaFarmaceutica.setAdapter(adapterForma);

                // Set selected item in the spinner
                for (int i = 0; i < formaFarmaceuticas.size(); i++) {
                    if (formaFarmaceuticas.get(i).getIdFormaFarmaceutica() == articulo.getIdFormaFarmaceutica()) {
                        spinnerItemFormaFarmaceutica.setSelection(i);
                        break;
                    }
                }
            }
        });

        // Populate EditTexts with current DetalleCompra data
        idArticulo.setText(String.valueOf(articulo.getIdArticulo()));
        name.setText(articulo.getNombreArticulo());
        description.setText(articulo.getDescripcionArticulo());
        isRestricted.setChecked(articulo.getRestringidoArticulo());
        price.setText(String.valueOf(articulo.getPrecioArticulo()));

        builder.show();
    }
    //Buscar
    private void buscarArticulo(int id) {
        articuloMySQLDAO.getArticuloMySQLSqlite(id, articulo -> {
            if(articulo != null) {
                viewArticuloMySQL(articulo);
            } else {
                Toast.makeText(this, R.string.not_found_message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}