/*
The contents of this file are subject to the Mozilla Public License Version 1.1 
(the "License"); you may not use this file except in compliance with the License. 
You may obtain a copy of the License at http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis, WITHOUT
WARRANTY OF ANY KIND, either express or implied. See the License for the specific 
language governing rights and limitations under the License.

The Original Code is "TimeSeries1DImpl.java". Description: 
"Default implementation of TimeSeries"

The Initial Developer of the Original Code is Bryan Tripp & Centre for Theoretical Neuroscience, University of Waterloo. Copyright (C) 2006-2008. All Rights Reserved.

Alternatively, the contents of this file may be used under the terms of the GNU 
Public License license (the GPL License), in which case the provisions of GPL 
License are applicable  instead of those above. If you wish to allow use of your 
version of this file only under the terms of the GPL License and not to allow 
others to use your version of this file under the MPL, indicate your decision 
by deleting the provisions above and replace  them with the notice and other 
provisions required by the GPL License.  If you do not delete the provisions above,
a recipient may use your version of this file under either the MPL or the GPL License.
*/

/*
 * Created on May 4, 2006
 */
package ca.nengo.util.impl;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.lang.reflect.Method;

import ca.nengo.model.Units;
import ca.nengo.util.TimeSeries1D;

/**
 * Default implementation of TimeSeries.  
 * 
 * @author Bryan Tripp
 */
public class TimeSeries1DImpl implements TimeSeries1D, Serializable, Parcelable {

	private static final long serialVersionUID = 1L;
	
	private float[] myTimes;
	private float[] myValues;
	private Units myUnits;
	private String myLabel;
	private String myName;
	
	/**
	 * @param times @see ca.bpt.cn.util.TimeSeries#getTimes()
	 * @param values @see ca.bpt.cn.util.TimeSeries#getValues()
	 * @param units @see ca.bpt.cn.util.TimeSeries#getUnits()
	 */	 
	public TimeSeries1DImpl(float[] times, float[] values, Units units) {
		if (times.length != values.length) {
			throw new IllegalArgumentException(times.length + " times were given with " + values.length + " values");
		}
		
		this.myTimes = times;
		this.myValues = values;
		this.myUnits = units;
		this.myLabel = "1";		
	}
	
	/**
	 * @see ca.nengo.util.TimeSeries#getName()
	 */
	public String getName() {
		return myName;
	}
	
	/**
	 * @param name Name of the TimeSeries
	 */
	public void setName(String name) {
		myName = name;
	}

	/**
	 * @see ca.nengo.util.TimeSeries1D#getTimes()
	 */
	public float[] getTimes() {
		return myTimes;		
	}
	
//	private void setTimes(float[] times) {
//		myTimes = times;
//	}

	/**
	 * @see ca.nengo.util.TimeSeries1D#getValues1D()
	 */
	public float[] getValues1D() {
		return myValues;
	}

	/**
	 * @see ca.nengo.util.TimeSeries1D#getUnits1D()
	 */
	public Units getUnits1D() {
		return myUnits;
	}

	/**
	 * @see ca.nengo.util.TimeSeries#getDimension()
	 */
	public int getDimension() {
		return 1;
	}

	/**
	 * @see ca.nengo.util.TimeSeries#getValues()
	 */
	public float[][] getValues() {
		float[][] result = new float[myValues.length][];
		
		for (int i = 0; i < myValues.length; i++) {
			result[i] = new float[]{myValues[i]};
		}
		
		return result;
	}
	
//	private void setValues(float[] values) {
//		myValues = values;
//	}

	/**
	 * @see ca.nengo.util.TimeSeries#getUnits()
	 */
	public Units[] getUnits() {
		return new Units[]{myUnits};
	}
	
	/**
	 * @param units New Units
	 */
	public void setUnits(Units units) {
		myUnits = units;
	}
	
	/**
	 * @see ca.nengo.util.TimeSeries#getLabels()
	 */
	public String[] getLabels() {
		return new String[]{myLabel};
	}
	
	/**
	 * @param label New label
	 */
	public void setLabel(String label) {
		myLabel = label;
	}

	@Override
	public TimeSeries1D clone() throws CloneNotSupportedException {
		return (TimeSeries1D) super.clone();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeFloatArray(this.myTimes);
		dest.writeFloatArray(this.myValues);
		dest.writeInt(this.myUnits == null ? -1 : this.myUnits.ordinal());
		dest.writeString(this.myLabel);
		dest.writeString(this.myName);
	}

	protected TimeSeries1DImpl(Parcel in) {
		this.myTimes = in.createFloatArray();
		this.myValues = in.createFloatArray();
		int tmpMyUnits = in.readInt();
		this.myUnits = tmpMyUnits == -1 ? null : Units.values()[tmpMyUnits];
		this.myLabel = in.readString();
		this.myName = in.readString();
	}

	public static final Parcelable.Creator<TimeSeries1DImpl> CREATOR = new Parcelable.Creator<TimeSeries1DImpl>() {
		@Override
		public TimeSeries1DImpl createFromParcel(Parcel source) {
			return new TimeSeries1DImpl(source);
		}

		@Override
		public TimeSeries1DImpl[] newArray(int size) {
			return new TimeSeries1DImpl[size];
		}
	};
}
