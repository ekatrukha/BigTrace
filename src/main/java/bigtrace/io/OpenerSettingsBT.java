package bigtrace.io;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Map;

import org.scijava.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import ch.epfl.biop.bdv.img.bioformats.BioFormatsOpener;
import ch.epfl.biop.bdv.img.omero.OmeroOpener;
import ch.epfl.biop.bdv.img.opener.Opener;
import ch.epfl.biop.bdv.img.opener.OpenerSettings;
import ch.epfl.biop.bdv.img.opener.OpenerSettings.OpenerType;
import ch.epfl.biop.bdv.img.qupath.QuPathOpener;
import net.imglib2.realtransform.AffineTransform3D;
import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.unit.Unit;

public class OpenerSettingsBT 
{
    static Logger logger = LoggerFactory.getLogger(OpenerSettings.class);

    transient Context scijavaContext;

    // --------- Extensibility
    String opt = ""; // options

    //---- Modifications on the location of the dataset ( pixel size, origin, flip)
    // all transient because they are used only on first initialisation,
    // after, all these modifications are stored and serialized in the view transforms
    transient double[] positionPreTransformMatrixArray = new AffineTransform3D().getRowPackedCopy();
    transient double[] positionPostTransformMatrixArray = new AffineTransform3D().getRowPackedCopy();
    transient boolean positionIsImageCenter = false; // Top left corner otherwise

    //---- Target unit : the unit in which the image will be opened
    transient Length defaultSpaceUnit = new Length(1,UNITS.MICROMETER);
    transient Length defaultVoxelUnit = new Length(1,UNITS.MICROMETER);
    String unit = "MICROMETER";//UnitsLength.MICROMETER.toString();

    //---- How to open the dataset (block size, number of readers per image)
    int nReader = 10; // parallel reading : number of pixel readers allowed
    boolean defaultBlockSize = true; // The block size chosen is let to be defined by the opener implementation itself
     int[] blockSize = new int[]{512,512,1};

    //-------- Channels options
    boolean splitRGB = false; // Should be true for 16 bits RGB channels like we have in CZI, Imglib2, the library used after, do not have a specific type class for 16 bits RGB pixels

    // ---- Opener core options
    OpenerType type = OpenerType.UNDEF;
    String location = "";

    // ---- For BioFormats: series index
    // ---- For QuPath: entryID
    int id = -1;

    // In case the opener can't be opened, we need at least to know the number of channels in order
    // to open a fake dataset on the next time
    int nChannels = -1;

    public OpenerSettingsBT positionConvention(String position_convention) {
        if (position_convention.equals("CENTER")) {
            return this.centerPositionConvention();
        }
        return this.cornerPositionConvention();
    }

    public OpenerType getType() {
        return type;
    }

    public String getLocation() {
        return location;
    }

    public int getNChannels() {
        return nChannels;
    }

    public int getEntryId() { return id; }
    public int getSeries() { return id; }

    public enum OpenerType {
        BIOFORMATS,
        OMERO,
        IMAGEJ,
        OPENSLIDE,
        QUPATH,
        UNDEF
    }

    public OpenerSettingsBT context(Context context) {
        this.scijavaContext = context;
        return this;
    }

    // ---- cache and readers
    public OpenerSettingsBT readerPoolSize(int pSize){
        this.nReader = pSize;
        return this;
    }

    public OpenerSettingsBT useDefaultCacheBlockSize(boolean flag) {
        defaultBlockSize = flag;
        return this;
    }

    public void setNChannels(int nChannels) {
        this.nChannels = nChannels;
    }

    public OpenerSettingsBT cacheBlockSize(int sx, int sy, int sz) {
        defaultBlockSize = false;
        blockSize = new int[]{sx,sy,sz}; //new FinalInterval(sx, sy, sz);
        return this;
    }

    // All space transformation methods
    public OpenerSettingsBT flipPositionXYZ() {
        if (this.positionPreTransformMatrixArray == null) {
            positionPreTransformMatrixArray = new AffineTransform3D()
                    .getRowPackedCopy();
        }
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.set(positionPreTransformMatrixArray);
        at3D.scale(-1);
        positionPreTransformMatrixArray = at3D.getRowPackedCopy();
        return this;
    }

    public OpenerSettingsBT flipPositionX() {
        if (this.positionPreTransformMatrixArray == null) {
            positionPreTransformMatrixArray = new AffineTransform3D()
                    .getRowPackedCopy();
        }
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.set(positionPreTransformMatrixArray);
        at3D.scale(-1, 1, 1);
        positionPreTransformMatrixArray = at3D.getRowPackedCopy();
        return this;
    }

    public OpenerSettingsBT flipPositionY() {
        if (this.positionPreTransformMatrixArray == null) {
            positionPreTransformMatrixArray = new AffineTransform3D()
                    .getRowPackedCopy();
        }
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.set(positionPreTransformMatrixArray);
        at3D.scale(1, -1, 1);
        positionPreTransformMatrixArray = at3D.getRowPackedCopy();
        return this;
    }

    public OpenerSettingsBT flipPositionZ() {
        if (this.positionPreTransformMatrixArray == null) {
            positionPreTransformMatrixArray = new AffineTransform3D()
                    .getRowPackedCopy();
        }
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.set(positionPreTransformMatrixArray);
        at3D.scale(1, 1, -1);
        positionPreTransformMatrixArray = at3D.getRowPackedCopy();
        return this;
    }

    public OpenerSettingsBT setPositionPreTransform(AffineTransform3D at3d) {
        positionPreTransformMatrixArray = at3d.getRowPackedCopy();
        return this;
    }

    public OpenerSettingsBT setPositionPostTransform(AffineTransform3D at3d) {
        positionPostTransformMatrixArray = at3d.getRowPackedCopy();
        return this;
    }

    public OpenerSettingsBT centerPositionConvention() {
        this.positionIsImageCenter = true;
        return this;
    }

    public OpenerSettingsBT cornerPositionConvention() {
        this.positionIsImageCenter = false;
        return this;
    }

    // reference frames
    public OpenerSettingsBT positionReferenceFrameLength(Length l)
    {
        this.defaultSpaceUnit = l;
        return this;
    }

    public OpenerSettingsBT voxSizeReferenceFrameLength(Length l)
    {
        this.defaultVoxelUnit = l;
        return this;
    }


    // data location
    public OpenerSettingsBT location(String location) {
        this.location = location;
        return this;
    }

    public OpenerSettingsBT location(URI uri) throws URISyntaxException {
        if(uri.getScheme().equals("https") || uri.getScheme().equals("http"))
            this.location = uri.toString();
        else {
            URI newuri = new URI(uri.getScheme(), uri.getHost(), uri.getPath(), null);
            this.location = Paths.get(newuri).toString();
        }
        return this;
    }

    public OpenerSettingsBT location(File f) {
        this.location = f.getAbsolutePath();
        return this;
    }


    // channels
    public OpenerSettingsBT splitRGBChannels() {
        splitRGB = true;
        return this;
    }

    // channels
    public OpenerSettingsBT splitRGBChannels(boolean flag) {
        splitRGB = flag;
        return this;
    }

   public OpenerSettingsBT unit(String u) {
       this.unit = u;
       return this;
    }

    public OpenerSettingsBT unit(UNITS u) {
        this.unit = u.getName();
        return this;
    }

    public OpenerSettingsBT unit(Unit<Length> u) {
        this.unit = u.getSymbol();
        return this;
    }

    public OpenerSettingsBT millimeter() {
        this.unit = UNITS.MILLIMETER.getSymbol();//UnitsLength.MILLIMETER.toString();
        return this;
    }

    public OpenerSettingsBT micrometer() {
        this.unit = UNITS.MICROMETER.getSymbol();//UnitsLength.MICROMETER.toString();
        return this;
    }

    public OpenerSettingsBT nanometer() {
        this.unit = UNITS.NANOMETER.getSymbol();//UnitsLength.NANOMETER.toString();
        return this;
    }


    // define which kind of builder to deal with
    public OpenerSettingsBT omeroBuilder(){
        this.type = OpenerType.OMERO;
        return this;
    }

    public OpenerSettingsBT bioFormatsBuilder(){
        this.type = OpenerType.BIOFORMATS;
        return this;
    }

    public OpenerSettingsBT imageJBuilder(){
        this.type = OpenerType.IMAGEJ;
        return this;
    }

    public OpenerSettingsBT openSlideBuilder(){
        this.type = OpenerType.OPENSLIDE;
        return this;
    }

    public OpenerSettingsBT quPathBuilder(){
        this.type = OpenerType.QUPATH;
        return this;
    }

    // BioFormats specific
    public OpenerSettingsBT setSerie(int iSerie){
        this.id = iSerie;
        return this;
    }

    public OpenerSettingsBT setEntry(int entryId){
        this.id = entryId;
        return this;
    }

    transient boolean skipMeta = false;

    public OpenerSettingsBT skipMeta() {
        this.skipMeta = true;
        return this;
    }

    public Opener<?> create(Map<String, Object> cachedObjects) throws Exception {
        Opener<?> opener;
        switch (this.type) {
            case OMERO:
                opener = new OmeroOpener(
                        scijavaContext,
                        location,
                        nReader,
                        unit,
                        positionIsImageCenter,
                        cachedObjects,
                        nChannels,
                        skipMeta
                );
                break;
            case QUPATH:
                opener = new QuPathOpener<>(
                        scijavaContext,
                        location,
                        id,
                        unit,
                        positionIsImageCenter,
                        nReader,
                        defaultBlockSize,
                        blockSize,
                        splitRGB,
                        cachedObjects,
                        nChannels, skipMeta);
                break;
            case BIOFORMATS:
                opener = new BioFormatsOpenerBT(
                        scijavaContext,
                        location,
                        id,
                        // Location of the image
                        positionPreTransformMatrixArray,
                        positionPostTransformMatrixArray,
                        positionIsImageCenter,
                        defaultSpaceUnit,
                        defaultVoxelUnit,
                        unit,
                        // How to stream it
                        nReader,
                        defaultBlockSize,
                        blockSize,
                        // Channel options
                        splitRGB,
                        cachedObjects,
                        nChannels, skipMeta
                );
                break;
            case IMAGEJ:
                throw new UnsupportedOperationException("ImageJ opener not supported");

            case OPENSLIDE:
                throw new UnsupportedOperationException("OPENSLIDE opener not supported");

            default:
                throw new UnsupportedOperationException(this.type +" opener not supported");
        }

        if (opener.getNChannels()!=-1) {
            nChannels = opener.getNChannels();
        }

        return opener;

    }

    public static OpenerSettingsBT BioFormats() {
        return new OpenerSettingsBT().bioFormatsBuilder();
    }

    public static OpenerSettingsBT OMERO() {
        return new OpenerSettingsBT().omeroBuilder();
    }

    public static OpenerSettingsBT QuPath() {
        return new OpenerSettingsBT().quPathBuilder();
    }

    /*public static OpenerSettings getDefaultSettings(OpenerType type){
        switch (type){
            case OMERO: return
            case IMAGEJ: return new OpenerSettings().imageJBuilder();
            case BIOFORMATS: return
            case OPENSLIDE: return new OpenerSettings().openSlideBuilder();
            case QUPATH: return
            default:
                logger.error("Unrecognized opener type "+ type);
                return null;
        }
    }*/

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
