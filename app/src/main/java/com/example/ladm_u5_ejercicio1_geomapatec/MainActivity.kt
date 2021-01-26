package com.example.ladm_u5_ejercicio1_geomapatec

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.view.get

import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {


    var baseRemota = FirebaseFirestore.getInstance()
    var res = ArrayList<String>()
    var posicion = ArrayList<Data>()
    lateinit var locacion :LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),1)
        }

        //NO ES UBICAION ACTUAL ES ULTIA CONOCIDA

        baseRemota.collection("tecnologico")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->

                    if(firebaseFirestoreException != null){
                        textView.setText("ERROR: "+firebaseFirestoreException)
                        return@addSnapshotListener
                    }
                    var resultado = ""
                    res.clear()
                    posicion.clear()
                    for(document in querySnapshot!!){
                        var data = Data()
                        data.nombre=document.getString("nombre").toString()
                        data.posicion1=document.getGeoPoint("posicion1")!!
                        data.posicion2=document.getGeoPoint("posicion2")!!

                        resultado += data.toString()+"\n\n"
                        posicion.add(data)
                        res.add(data.toString())
                    }
                    lista.adapter=ArrayAdapter<Data>(this,android.R.layout.simple_list_item_1,posicion)
                }
        locacion=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var oyente= ClassOyente(this)
        locacion.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,01f,oyente)

        button.setOnClickListener {
            //miUbicacion()
            insertarFiresbase()
        }
        lista.setOnItemClickListener { parent, view, position, id ->
            mostrarAlertEliminarActualizar(position)
        }

    }
    private fun mostrarAlertEliminarActualizar(posicion:Int) {
        var idLista = lista.get(posicion)
        AlertDialog.Builder(this)
            .setTitle("ATENCION")
            .setMessage("¿Que desea hacer con \n ${idLista}? ")
            .setPositiveButton("Eliminar"){d,i-> Eliminar(idLista.toString())}
            .setNeutralButton("CANCELAR")  {d,i->}
            .show()
    }
    private fun Eliminar(idLista: String) {
        baseRemota.collection("tecnologico")
            .document(idLista)
            .delete()
            .addOnFailureListener {
                mensaje("no se pudo eliminar")
            }
            .addOnSuccessListener {
                Toast.makeText(this, "Se elimino correctamente", Toast.LENGTH_SHORT).show()
            }
    }

    private fun insertarFiresbase() {
try {


        var Latitudposicion1= edtLatitudePosicion1.text.toString().toDouble()
        var Longitudposicion1= edtLongitudePosicion1.text.toString().toDouble()
        var posicion1= GeoPoint(Latitudposicion1,Longitudposicion1)
        var Latitudposicion2= edtLatitudePosicion2.text.toString().toDouble()
        var Longitudposicion2= edtLongitudePosicion2.text.toString().toDouble()
        var posicion2=GeoPoint(Latitudposicion2,Longitudposicion2)

        var datos = hashMapOf(
            "nombre" to edtGeoPoint.text.toString(),
            "posicion1" to posicion1,
            "posicion2" to posicion2
        )

        baseRemota.collection("tecnologico")
            .add(datos as Any)
            .addOnSuccessListener {
                Toast.makeText(this,"Se inserto datos", Toast.LENGTH_LONG).show()
                edtGeoPoint.setText("")
                edtLatitudePosicion1.setText("")
                edtLongitudePosicion1.setText("")
                edtLatitudePosicion2.setText("")
                edtLongitudePosicion2.setText("")
            }
            .addOnFailureListener {
                mensaje("No se pudo insertar \n" +
                        "${it.message}")
            }
    }catch (e:Exception){
    mensaje("Insertar Datos de Manera Correcta \n" +
            "${e.message}")
}
    }
    private fun mensaje(s: String) {
        AlertDialog.Builder(this)
            .setTitle("Atencion")
            .setMessage(s)
            .setPositiveButton("OK"){d,i->}
            .show()
    }
    private fun miUbicacion() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        LocationServices.getFusedLocationProviderClient(this)
            .lastLocation.addOnSuccessListener {
                var geoPosicion = GeoPoint(it.latitude,it.longitude)
                textView2.setText("${it.latitude},${it.longitude}")
                for(item in posicion)
                {
                    if(item.estoyEn(geoPosicion)){
                        AlertDialog.Builder(this)
                            .setMessage("USTED SE ESNCUENTRA EN: "+item.nombre)
                            .setTitle("ATENCION")
                            .setPositiveButton("OK"){q,p->}
                            .show()
                    }
                }
            }
            .addOnFailureListener {
                textView2.setText("ERROR AL OBTENER UBICACION")
            }
    }
}

class ClassOyente(puntero:MainActivity) : LocationListener{
    var p=puntero
    override fun onLocationChanged(location: Location) {
        p.textView2.setText("${location.latitude},${location.longitude}")
        p.textView3.setText("")
        var geoPosicion = GeoPoint(location.latitude,location.longitude)

        for(item in p.posicion){
            if(item.estoyEn(geoPosicion)){
                p.textView3.setText("Estas en: ${item.nombre}")
            }
        }

    }
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }
    override fun onProviderEnabled(provider: String) {

    }
    override fun onProviderDisabled(provider: String) {
    }

}
