/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2009 Vlad Skarzhevskyy
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 *  @author vlads
 *  @version $Id$
 */
package org.bluez.v4;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.bluetooth.DiscoveryAgent;

import org.bluez.BlueZAPI;
import org.bluez.Error.DoesNotExist;
import org.bluez.Error.InvalidArguments;
import org.bluez.Error.NoSuchAdapter;
import org.bluez.dbus.DBusProperties;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;

import com.intel.bluetooth.BluetoothConsts;

/**
 * Access BlueZ v4 over D-Bus
 */
public class BlueZAPIV4 implements BlueZAPI {

	private DBusConnection dbusConn;

	private ManagerV4 dbusManager;

	private AdapterV4 adapter;

	private Path adapterPath;

	public BlueZAPIV4(DBusConnection dbusConn, ManagerV4 dbusManager) {
		this.dbusConn = dbusConn;
		this.dbusManager = dbusManager;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#findAdapter(java.lang.String)
	 */
	public Path findAdapter(String pattern) throws InvalidArguments {
		try {
			return dbusManager.FindAdapter(pattern);
		} catch (NoSuchAdapter e) {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#defaultAdapter()
	 */
	public Path defaultAdapter() throws InvalidArguments {
		try {
			return dbusManager.DefaultAdapter();
		} catch (NoSuchAdapter e) {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#getAdapter(int)
	 */
	public Path getAdapter(int number) {
		Path[] adapters = dbusManager.ListAdapters();
		if (adapters == null) {
			throw null;
		}
		if ((number < 0) || (number >= adapters.length)) {
			throw null;
		}
		return adapters[number];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#listAdapters()
	 */
	public List<String> listAdapters() {
		List<String> v = new Vector<String>();
		Path[] adapters = dbusManager.ListAdapters();
		if (adapters != null) {
			for (int i = 0; i < adapters.length; i++) {
				String adapterId = String.valueOf(adapters[i]);
				final String bluezPath = "/org/bluez/";
				if (adapterId.startsWith(bluezPath)) {
					adapterId = adapterId.substring(bluezPath.length());
				}
				v.add(adapterId);
			}
		}
		return v;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#selectAdapter(org.freedesktop.dbus.Path)
	 */
	public org.bluez.Adapter selectAdapter(Path adapterPath) throws DBusException {
		adapter = dbusConn.getRemoteObject("org.bluez", adapterPath.getPath(), AdapterV4.class);
		this.adapterPath = adapterPath;
		return adapter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#getAdapterAddress()
	 */
	public String getAdapterAddress() {
		return DBusProperties.getStringValue(adapter, AdapterV4.Properties.Address);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bluez.BlueZAPI#getAdapterID()
	 */
	public String getAdapterID() {
		final String bluezPath = "/org/bluez/";
		if (adapterPath.getPath().startsWith(bluezPath)) {
			return adapterPath.getPath().substring(bluezPath.length());
		} else {
			return adapterPath.getPath();
		}
	}
	
    /* (non-Javadoc)
     * @see org.bluez.BlueZAPI#getAdapterDeviceClass()
     */
    public int getAdapterDeviceClass() {
        // TODO How do I get this in BlueZ 4?
        return BluetoothConsts.DeviceClassConsts.MAJOR_COMPUTER;
    }

    /* (non-Javadoc)
     * @see org.bluez.BlueZAPI#getAdapterName()
     */
    public String getAdapterName() {
        return DBusProperties.getStringValue(adapter, AdapterV4.Properties.Name);
    }

    /* (non-Javadoc)
     * @see org.bluez.BlueZAPI#isAdapterDiscoverable()
     */
    public boolean isAdapterDiscoverable() {
        return DBusProperties.getBooleanValue(adapter, AdapterV4.Properties.Discoverable);
    }
    
    /* (non-Javadoc)
     * @see org.bluez.BlueZAPI#getAdapterDiscoverableTimeout()
     */
    public int getAdapterDiscoverableTimeout() {
        return DBusProperties.getIntValue(adapter, AdapterV4.Properties.DiscoverableTimeout);
    }

    /* (non-Javadoc)
     * @see org.bluez.BlueZAPI#setAdapterDiscoverable(int)
     */
    public boolean setAdapterDiscoverable(int mode) throws DBusException {
        switch (mode) {
        case DiscoveryAgent.NOT_DISCOVERABLE:
            adapter.SetProperty(DBusProperties.getPropertyName(AdapterV4.Properties.Discoverable), new Variant<Boolean>(Boolean.FALSE));
            break;
        case DiscoveryAgent.GIAC:
            adapter.SetProperty(DBusProperties.getPropertyName(AdapterV4.Properties.DiscoverableTimeout), new Variant<UInt32>(new UInt32(0)));
            adapter.SetProperty(DBusProperties.getPropertyName(AdapterV4.Properties.Discoverable), new Variant<Boolean>(Boolean.TRUE));
            break;
        case DiscoveryAgent.LIAC:
            adapter.SetProperty(DBusProperties.getPropertyName(AdapterV4.Properties.DiscoverableTimeout), new Variant<UInt32>(new UInt32(180)));
            adapter.SetProperty(DBusProperties.getPropertyName(AdapterV4.Properties.Discoverable), new Variant<Boolean>(Boolean.TRUE));
            break;
        default:
            throw new IllegalArgumentException("Invalid discoverable mode");
        }
        return true;
    }
    
    /* (non-Javadoc)
     * @see org.bluez.BlueZAPI#getAdapterManufacturer()
     */
    public String getAdapterManufacturer() {
     // TODO How do I get this in BlueZ 4?
        return null;
    }

    /* (non-Javadoc)
     * @see org.bluez.BlueZAPI#getAdapterRevision()
     */
    public String getAdapterRevision() {
     // TODO How do I get this in BlueZ 4?
        return null;
    }

    /* (non-Javadoc)
     * @see org.bluez.BlueZAPI#getAdapterVersion()
     */
    public String getAdapterVersion() {
     // TODO How do I get this in BlueZ 4?
        return null;
    }

    /* (non-Javadoc)
     * @see org.bluez.BlueZAPI#isAdapterPowerOn()
     */
    public boolean isAdapterPowerOn() {
        return DBusProperties.getBooleanValue(adapter, AdapterV4.Properties.Powered);
    }

    /* (non-Javadoc)
     * @see org.bluez.BlueZAPI#deviceInquiry(org.bluez.BlueZAPI.DeviceInquiryListener)
     */
    public void deviceInquiry(final DeviceInquiryListener listener) throws DBusException, InterruptedException {
        DBusSigHandler<AdapterV4.DeviceFound> remoteDeviceFound = new DBusSigHandler<AdapterV4.DeviceFound>() {
            public void handle(AdapterV4.DeviceFound s) {
                String deviceName = DBusProperties.getStringValue(s.getDevicePoperties(), Device.Properties.Name);
                int deviceClass = DBusProperties.getIntValue(s.getDevicePoperties(), Device.Properties.Class);
                //TODO verify that this ever present
                boolean paired = DBusProperties.getBooleanValue(s.getDevicePoperties(), Device.Properties.Paired, false);
                listener.deviceDiscovered(s.getDeviceAddress(), deviceName, deviceClass, paired);
            }
        };
        try {
            dbusConn.addSigHandler(AdapterV4.DeviceFound.class, remoteDeviceFound);

            adapter.StartDiscovery();
            
            listener.deviceInquiryStarted();
            
            while (DBusProperties.getBooleanValue(adapter, AdapterV4.Properties.Discovering)) {
                Thread.sleep(200);
            }

            adapter.StopDiscovery();

        } finally {
            dbusConn.removeSigHandler(AdapterV4.DeviceFound.class, remoteDeviceFound);
        }
    }

    /* (non-Javadoc)
     * @see org.bluez.BlueZAPI#deviceInquiryCancel()
     */
    public void deviceInquiryCancel() throws DBusException {
        adapter.StopDiscovery();
    }

    /* (non-Javadoc)
     * @see org.bluez.BlueZAPI#getRemoteDeviceFriendlyName(java.lang.String)
     */
    public String getRemoteDeviceFriendlyName(String deviceAddress) throws DBusException, IOException {
        // TODO Auto-generated method stub
        return null;
    }
    
    private Device findDevice(String deviceAddress) throws DBusException {
        Path devicePath = adapter.FindDevice(deviceAddress);
        return dbusConn.getRemoteObject("org.bluez", devicePath.getPath(), Device.class);
    }
    
    private Device getDevice(String deviceAddress) throws DBusException {
        Variant<Path[]> devices = (Variant<Path[]>)adapter.GetProperties().get(DBusProperties.getPropertyName(AdapterV4.Properties.Devices));
        if (devices == null) {
            return null;
        }
        Path devicePath = adapter.FindDevice(deviceAddress);
        return dbusConn.getRemoteObject("org.bluez", devicePath.getPath(), Device.class);
    }

    /* (non-Javadoc)
     * @see org.bluez.BlueZAPI#isRemoteDeviceTrusted(java.lang.String)
     */
    public Boolean isRemoteDeviceTrusted(String deviceAddress) throws DBusException {
        return null;
        //TODO
        //return DBusProperties.getBooleanValue(findDevice(deviceAddress), Device.Properties.Paired);
    }

    /* (non-Javadoc)
     * @see org.bluez.BlueZAPI#getRemoteDeviceServices(java.lang.String)
     */
    public Map<Integer, String> getRemoteDeviceServices(String deviceAddress) throws DBusException {
        Path devicePath;
        try {
            devicePath = adapter.FindDevice(deviceAddress);
        } catch (DoesNotExist e) {
            devicePath = adapter.CreateDevice(deviceAddress);
        }
        Device device = dbusConn.getRemoteObject("org.bluez", devicePath.getPath(), Device.class);
        
        Map<UInt32, String> xmlMap =  device.DiscoverServices("");
        Map<Integer, String> xmlRecords = new HashMap<Integer, String>();
        for (Map.Entry<UInt32, String> record : xmlMap.entrySet()) {
            xmlRecords.put(record.getKey().intValue(), record.getValue());
        }
        return xmlRecords;

    }
    

}