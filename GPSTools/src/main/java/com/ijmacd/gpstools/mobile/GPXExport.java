package com.ijmacd.gpstools.mobile;

import android.database.Cursor;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created with IntelliJ IDEA.
 * User: Iain
 * Date: 02/07/13
 * Time: 00:09
 * To change this template use File | Settings | File Templates.
 */
public class GPXExport {
    public static boolean exportGPX(Track track, File directory){
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.newDocument();
            String ns = "http://www.topografix.com/GPX/1/1";

            Element rootElement = document.createElementNS(ns, "gpx");
            rootElement.setAttribute("version", "1.1");
            rootElement.setAttribute("creator", "GPSTools Mobile");
            document.appendChild(rootElement);

            Element trackElement = document.createElementNS(ns, "trk");
            rootElement.appendChild(trackElement);

            Element trackSegmentElement = document.createElementNS(ns, "trkseg");
            trackElement.appendChild(trackSegmentElement);

            final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date startDate = null;

            Cursor cursor = track.getPoints();
            while(cursor.moveToNext()){
                final float lat = cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.LAT_COLUMN));
                final float lon = cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.LON_COLUMN));
                final float ele = cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.ALTITUDE_COLUMN));
                final long time = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.DATE_COLUMN));
                final double cadence = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.CSC_CADENCE_COLUMN));
                final Date date = new Date(time);

                if(startDate == null){
                    startDate = date;
                }


                Element trackPointElement = document.createElementNS(ns, "trkpt");
                trackPointElement.setAttribute("lat", String.valueOf(lat));
                trackPointElement.setAttribute("lon", String.valueOf(lon));
                trackSegmentElement.appendChild(trackPointElement);

                Element elevationElement = document.createElementNS(ns, "ele");
                elevationElement.appendChild(document.createTextNode(String.valueOf(ele)));
                trackPointElement.appendChild(elevationElement);

                Element timeElement = document.createElementNS(ns, "time");
                timeElement.appendChild(document.createTextNode(formatter.format(date)));
                trackPointElement.appendChild(timeElement);

                Element extensionsElement = document.createElementNS(ns, "extensions");

                Element cadenceElement = document.createElementNS(ns, "cadence");
                cadenceElement.appendChild(document.createTextNode(String.valueOf(cadence)));

                extensionsElement.appendChild(cadenceElement);
                trackPointElement.appendChild(extensionsElement);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            DOMSource source = new DOMSource(document.getDocumentElement());

            SimpleDateFormat nameFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss'Z'");
            nameFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            final String filename = nameFormatter.format(startDate) + ".gpx";
            File file = new File(directory, filename);
            FileOutputStream output = new FileOutputStream(file);

            StreamResult result = new StreamResult(output);

            transformer.transform(source, result);

            return true;


        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
