/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.detectors;

import java.awt.BorderLayout;
import javax.swing.JSplitPane;
import org.clas.viewer.DetectorMonitor;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.view.DetectorShape2D;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataBank;
import org.jlab.rec.dc.Constants;
import org.jlab.rec.dc.GeometryLoader;

/**
 *
 * @author devita
 */
public class DCmonitor extends DetectorMonitor {
    

    public DCmonitor(String name) {
        super(name);
        
        this.init();
    }

    
    @Override
    public void createHistos() {
        // initialize canvas and create histograms
        this.setNumberOfEvents(0);
        this.getDetectorCanvas().divide(2, 3);
        this.getDetectorCanvas().setGridX(false);
        this.getDetectorCanvas().setGridY(false);
        H1F summary = new H1F("summary","summary",6,1,7);
        summary.setTitleX("sector");
        summary.setTitleY("DC hits");
        summary.setFillColor(33);
        DataGroup sum = new DataGroup(1,1);
        sum.addDataSet(summary, 0);
        this.setDetectorSummary(sum);
        for(int sector=1; sector <= 6; sector++) {
            H2F raw = new H2F("raw", "Sector " + sector + " Occupancy", 112, 1, 113, 36, 1, 37.);
            raw.setTitleX("wire");
            raw.setTitleY("layer");
            raw.setTitle("sector "+sector);
            H2F occ = new H2F("occ", "Sector " + sector + " Occupancy", 112, 1, 113, 36, 1, 37.);
            occ.setTitleX("wire");
            occ.setTitleY("layer");
            occ.setTitle("sector "+sector);
            
            DataGroup dg = new DataGroup(2,1);
            dg.addDataSet(raw, 0);
            dg.addDataSet(occ, 1);
            this.getDataGroup().add(dg, sector,0,0);
        }

    }

    public void drawDetector() {
        // Load the Constants
        Constants.Load(true, true, 0);
        GeometryLoader.Load(10, "default");

        for(int s =0; s< 6; s++)
            for(int slrnum = 5; slrnum > -1; slrnum--) {
                //DetectorShape2D module = new DetectorShape2D(DetectorType.DC,s,slrnum,0);

                 DetectorShape2D module = new DetectorShape2D();
                 module.getDescriptor().setType(DetectorType.DC);
                 module.getDescriptor().setSectorLayerComponent((s+1), (slrnum+1), 1);

                module.getShapePath().addPoint(GeometryLoader.dcDetector.getSector(0).getSuperlayer(slrnum).getLayer(0).getComponent(0).getLine().origin().x(),  
                        -GeometryLoader.dcDetector.getSector(0).getSuperlayer(slrnum).getLayer(0).getComponent(0).getLine().origin().y(),  0.0);
                module.getShapePath().addPoint(GeometryLoader.dcDetector.getSector(0).getSuperlayer(slrnum).getLayer(0).getComponent(0).getLine().end().x(),  
                        -GeometryLoader.dcDetector.getSector(0).getSuperlayer(slrnum).getLayer(0).getComponent(0).getLine().end().y(),  0.0);
                module.getShapePath().addPoint(GeometryLoader.dcDetector.getSector(0).getSuperlayer(slrnum).getLayer(0).getComponent(111).getLine().end().x(),  
                        -GeometryLoader.dcDetector.getSector(0).getSuperlayer(slrnum).getLayer(0).getComponent(111).getLine().end().y(),  0.0);
                module.getShapePath().addPoint(GeometryLoader.dcDetector.getSector(0).getSuperlayer(slrnum).getLayer(0).getComponent(111).getLine().origin().x(),  
                        -GeometryLoader.dcDetector.getSector(0).getSuperlayer(slrnum).getLayer(0).getComponent(111).getLine().origin().y(),  0.0);


                if(slrnum%2==1)
                        module.setColor(180-slrnum*15,180,255);
                if(slrnum%2==0)
                        module.setColor(255-slrnum*15,182,229, 200);

                module.getShapePath().translateXYZ(110.0+((int)(slrnum/2))*50, 0, 0);
                module.getShapePath().rotateZ(s*Math.toRadians(60.));

                this.getDetectorView().getView().addShape("DC",module);			

            }
        this.getDetectorView().setName("DC"); 
        //detectorViewDC.updateBox();
    }

    @Override
    public void init() {
        this.getDetectorPanel().setLayout(new BorderLayout());
        this.drawDetector();
        JSplitPane   splitPane = new JSplitPane();
        splitPane.setLeftComponent(this.getDetectorView());
        splitPane.setRightComponent(this.getDetectorCanvas());
        this.getDetectorPanel().add(splitPane,BorderLayout.CENTER);  
        this.createHistos();
    }
        
    @Override
    public void processEvent(DataEvent event) {
        // process event info and save into data group
        if(event.hasBank("DC::dgtz")==true){
	    EvioDataBank bank = (EvioDataBank) event.getBank("DC::dgtz");
	    int rows = bank.rows();
	    for(int loop = 0; loop < rows; loop++){
                int sector = bank.getInt("sector", loop);
                int layer  = bank.getInt("layer", loop);
                int wire   = bank.getInt("wire", loop);
                this.getDataGroup().getItem(sector,0,0).getH2F("raw").fill(wire*1.0,layer*1.0);
                this.getDetectorSummary().getH1F("summary").fill(sector*1.0);
	    }
    	}
        
    }

    @Override
    public void resetEventListener() {
        System.out.println("Resetting DC histogram");
        this.createHistos();
    }

    @Override
    public void timerUpdate() {
///        System.out.println("Updating DC canvas");
        for(int sector=1; sector <=6; sector++) {
            H2F raw = this.getDataGroup().getItem(sector,0,0).getH2F("raw");
            for(int loop = 0; loop < raw.getDataBufferSize(); loop++){
                this.getDataGroup().getItem(sector,0,0).getH2F("occ").setDataBufferBin(loop,100*raw.getDataBufferBin(loop)/this.getNumberOfEvents());
            }
            this.getDetectorCanvas().cd(sector-1);
            this.getDetectorCanvas().draw(this.getDataGroup().getItem(sector,0,0).getH2F("occ"));
        }
        this.getDetectorCanvas().update();
    }

 

}