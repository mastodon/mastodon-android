package org.joinmastodon.android.ui.utils;

import android.os.SystemClock;

public class TransferSpeedTracker{
   private final double SMOOTHING_FACTOR=0.05;

   private long lastKnownPos;
   private long lastKnownPosTime;
   private double lastSpeed;
   private double averageSpeed;
   private long totalBytes;

   public void addSample(long position){
      if(lastKnownPosTime==0){
         lastKnownPosTime=SystemClock.uptimeMillis();
         lastKnownPos=position;
      }else{
         long time=SystemClock.uptimeMillis();
         lastSpeed=(position-lastKnownPos)/((double)(time-lastKnownPosTime)/1000.0);
         lastKnownPos=position;
         lastKnownPosTime=time;
      }
   }

   public double getLastSpeed(){
      return lastSpeed;
   }

   public double getAverageSpeed(){
      return averageSpeed;
   }

   public long updateAndGetETA(){ // must be called at a constant interval
      if(averageSpeed==0.0)
         averageSpeed=lastSpeed;
      else
         averageSpeed=SMOOTHING_FACTOR*lastSpeed+(1.0-SMOOTHING_FACTOR)*averageSpeed;
      return Math.round((totalBytes-lastKnownPos)/averageSpeed);
   }

   public void setTotalBytes(long totalBytes){
      this.totalBytes=totalBytes;
   }

   public void reset(){
      lastKnownPos=lastKnownPosTime=0;
      lastSpeed=averageSpeed=0.0;
      totalBytes=0;
   }
}
