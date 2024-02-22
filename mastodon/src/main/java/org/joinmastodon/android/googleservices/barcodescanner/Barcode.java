package org.joinmastodon.android.googleservices.barcodescanner;

import android.graphics.Point;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class Barcode extends AutoSafeParcelable{
	public static final int FORMAT_UNKNOWN = -1;
	public static final int FORMAT_ALL_FORMATS = 0;
	public static final int FORMAT_CODE_128 = 1;
	public static final int FORMAT_CODE_39 = 2;
	public static final int FORMAT_CODE_93 = 4;
	public static final int FORMAT_CODABAR = 8;
	public static final int FORMAT_DATA_MATRIX = 16;
	public static final int FORMAT_EAN_13 = 32;
	public static final int FORMAT_EAN_8 = 64;
	public static final int FORMAT_ITF = 128;
	public static final int FORMAT_QR_CODE = 256;
	public static final int FORMAT_UPC_A = 512;
	public static final int FORMAT_UPC_E = 1024;
	public static final int FORMAT_PDF417 = 2048;
	public static final int FORMAT_AZTEC = 4096;
	public static final int TYPE_UNKNOWN = 0;
	public static final int TYPE_CONTACT_INFO = 1;
	public static final int TYPE_EMAIL = 2;
	public static final int TYPE_ISBN = 3;
	public static final int TYPE_PHONE = 4;
	public static final int TYPE_PRODUCT = 5;
	public static final int TYPE_SMS = 6;
	public static final int TYPE_TEXT = 7;
	public static final int TYPE_URL = 8;
	public static final int TYPE_WIFI = 9;
	public static final int TYPE_GEO = 10;
	public static final int TYPE_CALENDAR_EVENT = 11;
	public static final int TYPE_DRIVER_LICENSE = 12;

	@SafeParceled(1)
	public int format;
	@SafeParceled(2)
	public String displayValue;
	@SafeParceled(3)
	public String rawValue;
	@SafeParceled(4)
	public byte[] rawBytes;
	@SafeParceled(5)
	public Point[] cornerPoints;
	@SafeParceled(6)
	public int valueType;
	@SafeParceled(7)
	public Email emailValue;
	@SafeParceled(8)
	public Phone phoneValue;
	@SafeParceled(9)
	public SMS smsValue;
	@SafeParceled(10)
	public WiFi wifiValue;
	@SafeParceled(11)
	public UrlBookmark urlBookmarkValue;
	@SafeParceled(12)
	public GeoPoint geoPointValue;
	@SafeParceled(13)
	public CalendarEvent calendarEventValue;
	@SafeParceled(14)
	public ContactInfo contactInfoValue;
	@SafeParceled(15)
	public DriverLicense driverLicenseValue;

	public static final Creator<Barcode> CREATOR=new AutoCreator<>(Barcode.class);

	// None of the following is needed or used in the Mastodon app and its use cases for QR code scanning,
	// but I'm putting it out there in case someone else is crazy enough to want to use Google Services without their libraries

	public static class Email extends AutoSafeParcelable{
		@SafeParceled(1)
		public int type;
		@SafeParceled(2)
		public String address;
		@SafeParceled(3)
		public String subject;
		@SafeParceled(4)
		public String body;

		public static final Creator<Email> CREATOR=new AutoCreator<>(Email.class);
	}

	public static class Phone extends AutoSafeParcelable{
		@SafeParceled(1)
		public int type;
		@SafeParceled(2)
		public String number;

		public static final Creator<Phone> CREATOR=new AutoCreator<>(Phone.class);
	}

	public static class SMS extends AutoSafeParcelable{
		@SafeParceled(1)
		public String message;
		@SafeParceled(2)
		public String phoneNumber;

		public static final Creator<SMS> CREATOR=new AutoCreator<>(SMS.class);
	}

	public static class WiFi extends AutoSafeParcelable{
		@SafeParceled(1)
		public String ssid;
		@SafeParceled(2)
		public String password;
		@SafeParceled(3)
		public int encryptionType;

		public static final Creator<WiFi> CREATOR=new AutoCreator<>(WiFi.class);
	}

	public static class UrlBookmark extends AutoSafeParcelable{
		@SafeParceled(1)
		public String title;
		@SafeParceled(2)
		public String url;

		public static final Creator<UrlBookmark> CREATOR=new AutoCreator<>(UrlBookmark.class);
	}

	public static class GeoPoint extends AutoSafeParcelable{
		@SafeParceled(1)
		public double lat;
		@SafeParceled(2)
		public double lng;

		public static final Creator<GeoPoint> CREATOR=new AutoCreator<>(GeoPoint.class);
	}

	public static class EventDateTime extends AutoSafeParcelable{
		@SafeParceled(1)
		public int year;
		@SafeParceled(2)
		public int month;
		@SafeParceled(3)
		public int day;
		@SafeParceled(4)
		public int hours;
		@SafeParceled(5)
		public int minutes;
		@SafeParceled(6)
		public int seconds;
		@SafeParceled(7)
		public boolean isUtc;
		@SafeParceled(8)
		public String rawValue;

		public static final Creator<EventDateTime> CREATOR=new AutoCreator<>(EventDateTime.class);
	}

	public static class CalendarEvent extends AutoSafeParcelable{
		@SafeParceled(1)
		public String summary;
		@SafeParceled(2)
		public String description;
		@SafeParceled(3)
		public String location;
		@SafeParceled(4)
		public String organizer;
		@SafeParceled(5)
		public String status;
		@SafeParceled(6)
		public EventDateTime start;
		@SafeParceled(7)
		public EventDateTime end;

		public static final Creator<CalendarEvent> CREATOR=new AutoCreator<>(CalendarEvent.class);
	}

	public static class Address extends AutoSafeParcelable{
		@SafeParceled(1)
		public int type;
		@SafeParceled(2)
		public String[] addressLines;

		public static final Creator<Address> CREATOR=new AutoCreator<>(Address.class);
	}

	public static class PersonName extends AutoSafeParcelable{
		@SafeParceled(1)
		public String formattedName;
		@SafeParceled(2)
		public String pronunciation;
		@SafeParceled(3)
		public String prefix;
		@SafeParceled(4)
		public String first;
		@SafeParceled(5)
		public String middle;
		@SafeParceled(6)
		public String last;
		@SafeParceled(7)
		public String suffix;

		public static final Creator<PersonName> CREATOR=new AutoCreator<>(PersonName.class);
	}

	public static class ContactInfo extends AutoSafeParcelable{
		@SafeParceled(1)
		public PersonName name;
		@SafeParceled(2)
		public String organization;
		@SafeParceled(3)
		public String title;
		@SafeParceled(4)
		public Phone[] phones;
		@SafeParceled(5)
		public Email[] emails;
		@SafeParceled(6)
		public String[] urls;
		@SafeParceled(7)
		public Address[] addresses;

		public static final Creator<ContactInfo> CREATOR=new AutoCreator<>(ContactInfo.class);
	}

	public static class DriverLicense extends AutoSafeParcelable{
		@SafeParceled(1)
		public String documentType;
		@SafeParceled(2)
		public String firstName;
		@SafeParceled(3)
		public String middleName;
		@SafeParceled(4)
		public String lastName;
		@SafeParceled(5)
		public String gender;
		@SafeParceled(6)
		public String addressStreet;
		@SafeParceled(7)
		public String addressCity;
		@SafeParceled(8)
		public String addressState;
		@SafeParceled(9)
		public String addressZip;
		@SafeParceled(10)
		public String licenseNumber;
		@SafeParceled(11)
		public String issueDate;
		@SafeParceled(12)
		public String expiryDate;
		@SafeParceled(13)
		public String birthDate;
		@SafeParceled(14)
		public String issuingCountry;

		public static final Creator<DriverLicense> CREATOR=new AutoCreator<>(DriverLicense.class);
	}
}
