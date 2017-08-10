package edu.cmu.sei.ttg.aaiot.client.cc2531;

import javax.usb.*;
import java.util.List;
//
///**
// * Created by sebastianecheverria on 7/18/17.
// */
//public class CC2531Controller {
//    int TI_USB_VENDOR_ID = 0x0451;
//    int TI_CC_2531_USB_ID = 0x16ae;
//
//    public CC2531Controller() throws UsbException
//    {
//        final UsbServices services = UsbHostManager.getUsbServices();
//        UsbDevice device = findDevice(services.getRootUsbHub(), TI_USB_VENDOR_ID, TI_CC_2531_USB_ID);
//        System.out.println("Device found: " + device);
//
//        // Dump information about the device itself
//        System.out.println(device);
//        final UsbPort port = device.getParentUsbPort();
//        if (port != null)
//        {
//            System.out.println("Connected to port: " + port.getPortNumber());
//            System.out.println("Parent: " + port.getUsbHub());
//        }
//
//        // Dump device descriptor
//        System.out.println(device.getUsbDeviceDescriptor());
//
//        // Process all configurations
//        for (UsbConfiguration configuration: (List<UsbConfiguration>) device.getUsbConfigurations())
//        {
//            // Dump configuration descriptor
//            System.out.println(configuration.getUsbConfigurationDescriptor());
//
//            // Process all interfaces
//            for (UsbInterface iface: (List<UsbInterface>) configuration.getUsbInterfaces())
//            {
//                // Dump the interface descriptor
//                System.out.println(iface.getUsbInterfaceDescriptor());
//
//                // Process all endpoints
//                for (UsbEndpoint endpoint: (List<UsbEndpoint>) iface.getUsbEndpoints())
//                {
//                    // Dump the endpoint descriptor
//                    System.out.println(endpoint.getUsbEndpointDescriptor());
//                }
//            }
//        }
//    }
//
//    public UsbDevice findDevice(UsbHub hub, int vendorId, int productId)
//    {
//        for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices())
//        {
//            UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
//            if (desc.idVendor() == vendorId && desc.idProduct() == productId) return device;
//            if (device.isUsbHub())
//            {
//                device = findDevice((UsbHub) device, vendorId, productId);
//                if (device != null) return device;
//            }
//        }
//        return null;
//    }
//
//
//
//}
