package io.transwarp.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
TranslateToMbtiles
作者：hanchun
2018-12-18
 */
public class TranslateToMbtiles {

    /*

     */
    private static void shpToGeoJson(String shpFilePath) throws IOException {
        File shpFile=new File(shpFilePath);

        if(!shpFile.exists()){
            throw new IOException("No such shapefile!!Check the path please!");
        }

        String shpDirPath=shpFile.getParentFile().getAbsolutePath();
        String shpKind=shpFile.getParentFile().getName();
        String shpFileName=shpFile.getName();

        String gsonFileName=shpKind+"_"+shpFileName.substring(0,shpFileName.lastIndexOf('.'))+".json";
        File geojsonFile=new File(shpDirPath+"/"+gsonFileName);
        if(geojsonFile.exists()){
            geojsonFile.delete();
        }

        translateShpToGeoJson(shpDirPath,gsonFileName,shpFileName);

    }

    private static void translateShpToGeoJson(String shpDirPath, String gsonFileName, String shpFileName){
        String prefix="docker run --mount type=bind,source="+shpDirPath+",target=/tmp klokantech/gdal ";
        StringBuilder sb=new StringBuilder();
        sb.append(prefix);
        sb.append("ogr2ogr -f GeoJSON /tmp/");
        sb.append(gsonFileName);
        sb.append(" -t_srs EPSG:3857");
        sb.append(" /tmp/");
        sb.append(shpFileName);

        try {
            execFromRoot(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    将一个文件夹中所有的geojson文件，集合生成同一个mbtiles,同一个layer
     */
    private static void geoJsonToMbtiles(String geoJsonDirPath,String layerName){
        String prefix="docker run --mount type=bind,source="+geoJsonDirPath+",target=/tmp klokantech/tippecanoe ";
        StringBuilder sb=new StringBuilder();
        sb.append(prefix);

        sb.append("tippecanoe --projection=EPSG:3857 -zg -o ");
        sb.append("/tmp/");
        sb.append(layerName);
        sb.append(".mbtiles ");
        sb.append("-l ");
        sb.append(layerName+" ");

        List<String> geoJsonPaths=new ArrayList<String>();

        File geoJsonDir=new File(geoJsonDirPath);
        File files[]=geoJsonDir.listFiles();
        for(File f:files){
            if(f.getName().endsWith(".json")){
                geoJsonPaths.add("/tmp/"+f.getName());
            }
        }

        for(String geoJsonPath:geoJsonPaths){
            sb.append(geoJsonPath+" ");
        }

        try {
            execFromRoot(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void execFromRoot(String cmd) throws Exception{
        byte[] passwd = ("123"+"\n").getBytes();
        Process process = Runtime.getRuntime().exec(new String[]{"sh","-c","sudo -S "+cmd});
        process.getOutputStream().write(passwd);
        process.getOutputStream().flush();
        try {
            int result=process.waitFor();
            System.out.println(" result="+result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void main(String args[]) throws Exception{
        String dataDirPath="/home/hanchun/data/new_user_data";

        File dataDirs[]=new File(dataDirPath).listFiles();
        for(File dataDir:dataDirs){
            if(!dataDir.isDirectory()) continue;

            File dataFiles[]=dataDir.listFiles();
            String dataDirAbsolutePath= dataDir.getAbsolutePath();
            for(File dataFile:dataFiles) {
                String dataFileName=dataFile.getName();
                if(dataFileName.endsWith(".shp")) {
                    shpToGeoJson(dataDirAbsolutePath+"/"+dataFileName);
                }
            }
        }


        for(File dataDir:dataDirs){
            String layerName=dataDir.getName();
            geoJsonToMbtiles(dataDir.getAbsolutePath(),layerName);
        }

    }
}
