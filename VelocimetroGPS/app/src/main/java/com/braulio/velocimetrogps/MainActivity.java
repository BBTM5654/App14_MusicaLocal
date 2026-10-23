package com.braulio.velocimetrogps;
import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private VelocimetroView velocimetro;
    private TextView txtVelocidad,txtGPS,txtPrecision,txtDescartados,txtContador;
    private Button btnIniciar,btnDetener;
    private LocationManager locationManager;
    private LocationListener listener;
    private double estimacion=0,varianza=25;
    private long tiempoAnterior=0;
    private int descartadas=0;
    private boolean inicializado=false,activo=false;
    private final StringBuilder historial=new StringBuilder();
    private static final int PERMISO_GPS=100;
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);setContentView(R.layout.activity_main);
        velocimetro=findViewById(R.id.velocimetro);
        txtVelocidad=findViewById(R.id.txtVelocidad);
        txtGPS=findViewById(R.id.txtGPS);
        txtPrecision=findViewById(R.id.txtPrecision);
        txtDescartados=findViewById(R.id.txtDescartados);
        txtContador=findViewById(R.id.txtContador);
        btnIniciar=findViewById(R.id.btnIniciar);
        btnDetener=findViewById(R.id.btnDetener);
        locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);
        listener=this::procesarUbicacion;
        btnIniciar.setOnClickListener(v->iniciar());
        btnDetener.setOnClickListener(v->detener());
    }
    private void iniciar(){
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISO_GPS);return;
        }
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            txtGPS.setText("Activa el GPS del teléfono");return;
        }
        estimacion=0;varianza=25;tiempoAnterior=0;inicializado=false;
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,500,0,listener);
            activo=true;btnIniciar.setEnabled(false);btnDetener.setEnabled(true);
            txtGPS.setText("Buscando señal GPS...");
        }catch(SecurityException e){txtGPS.setText("Sin permiso de ubicación");}
    }
    private void procesarUbicacion(Location loc){
        if(!activo)return;
        if(!loc.hasSpeed()){txtGPS.setText("Esperando velocidad del GPS...");return;}
        double medida=loc.getSpeed()*3.6;
        double errorVelocidad=loc.hasSpeedAccuracy()?loc.getSpeedAccuracyMetersPerSecond()*3.6:5.0;
        errorVelocidad=Math.max(2.0,errorVelocidad);
        double precision=loc.hasAccuracy()?loc.getAccuracy():-1;
        txtGPS.setText(String.format(Locale.US,"GPS sin filtrar: %.1f km/h",medida));
        txtPrecision.setText(precision>=0?String.format(Locale.US,"Precisión horizontal: ±%.1f m",precision):"Precisión: no disponible");
        long ahora=SystemClock.elapsedRealtime();
        if(!inicializado){
            estimacion=medida;inicializado=true;tiempoAnterior=ahora;mostrarVelocidad();return;
        }
        double dt=Math.max(0.1,Math.min(10.0,(ahora-tiempoAnterior)/1000.0));
        tiempoAnterior=ahora;
        varianza+=9.0*dt*dt;
        double r=errorVelocidad*errorVelocidad;
        double diferencia=Math.abs(medida-estimacion);
        double umbral=Math.max(12.0,3.0*Math.sqrt(varianza+r));
        if(diferencia>umbral){registrarDescartado(medida,diferencia);mostrarVelocidad();return;}
        double ganancia=varianza/(varianza+r);
        estimacion+=ganancia*(medida-estimacion);
        varianza=(1.0-ganancia)*varianza;
        mostrarVelocidad();
    }
    private void mostrarVelocidad(){
        float v=(float)Math.max(0,estimacion);
        velocimetro.setVelocidad(v);
        txtVelocidad.setText(String.format(Locale.US,"%.1f km/h",v));
        velocimetro.setContentDescription(String.format(Locale.US,"Velocidad estimada %.1f kilómetros por hora",v));
    }
    private void registrarDescartado(double medida,double diferencia){
        descartadas++;
        String hora=new SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(new Date());
        historial.insert(0,String.format(Locale.US,"%s | %.1f km/h\nSalto sospechoso: %.1f km/h\n\n",hora,medida,diferencia));
        if(historial.length()>5000)historial.setLength(5000);
        txtDescartados.setText(historial.toString());
        txtContador.setText("Total descartadas: "+descartadas);
    }
    private void detener(){
        if(locationManager!=null&&listener!=null)locationManager.removeUpdates(listener);
        activo=false;btnIniciar.setEnabled(true);btnDetener.setEnabled(false);txtGPS.setText("GPS detenido");
    }
    @Override protected void onStop(){super.onStop();detener();}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==PERMISO_GPS){
            if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)iniciar();
            else txtGPS.setText("Permiso de ubicación denegado");
        }
    }
}
