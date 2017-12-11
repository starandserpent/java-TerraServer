package com.ritualsoftheold.terra.net.codec;

import com.esotericsoftware.kryo.io.Output;

import io.vertx.core.buffer.Buffer;

/**
 * Output for Kryo that implements Vert.x buffer as backend.
 *
 */
public class VertxBufferOutput extends Output {
    // TODO override every single method from Output
    // They operate on output stream or byte array - we don't want that
    
    /**
     * Vert.x buffer. All operations should be done to this buffer.
     */
    private Buffer buffer;
    
    public VertxBufferOutput(Buffer buffer) {
        this.buffer = buffer;
    }
   
    public Buffer getBuff() {
    	return buffer;
    }
    
   public void setBuffer(Buffer buffer) {
	   this.buffer = buffer;
   }
    
   public byte[] toBytes() {
	   return buffer.getBytes();
   }
   
   public void write(int value) {
	   buffer.appendByte((byte)value);
   }
   
   public void write(byte[] bytes) {
	   buffer.appendBytes(bytes);
   }
   
   public void writeByte(byte value) {
	   buffer.appendByte(value);
   }
   
   public void writeByte(int value) {
	   buffer.appendByte((byte)value);
   }
   
   /** Writes a 4 byte int. Uses BIG_ENDIAN byte order. */
   public void writeInt(int value) {
	   writeByte((byte)(value >> 24));
	   writeByte((byte)(value >> 16));
	   writeByte((byte)(value >> 8));
	   writeByte((byte)value);
   }
   
	public int writeVarInt(int value, boolean optimizePositive) {
		if(!optimizePositive) {
			value = (value << 1) ^ (value >> 31);
		}
		if(value >>> 7 == 0) {
			writeByte((byte)value);
			return 1;
		}
		if(value >>> 14 == 0) {
			writeByte((byte)((value & 0x7F) | 0x80));
			writeByte((byte)(value >>> 7));
			return 2;
		}
		if(value >>> 21 == 0) {
			writeByte((byte)((value & 0x7F) | 0x80));
			writeByte((byte)(value >>> 7 | 0x80));
			writeByte((byte)(value >>> 14));
			return 3;
		}
		if(value >>> 28 == 0) {
			writeByte((byte)((value & 0x7F) | 0x80));
			writeByte((byte)(value >>> 7 | 0x80));
			writeByte((byte)(value >>> 14 | 0x80));
			writeByte((byte)(value >>> 21));
			return 4;
		}
		writeByte((byte)((value & 0x7F) | 0x80));
		writeByte((byte)(value >>> 7 | 0x80));
		writeByte((byte)(value >>> 14 | 0x80));
		writeByte((byte)(value >>> 21 | 0x80));
		writeByte((byte)(value >>> 28));
		return 5;
	}
	
	public void writeString(String value) {
		if(value == null) {
			writeByte(0x80);
			return;
		}
		int charCount = value.length();
		if(charCount == 0) {
			writeByte(1 | 0x80);
			return;
		}
		boolean ascii = false;
		if(charCount > 1 && charCount < 64) {
			ascii = true;
			for(int i = 0; i < charCount; i++) {
				int c = value.charAt(i);
				if(c > 127) {
					ascii = false;
					break;
				}
			}
		}
		if(ascii) {
			writeAscii_slow(value, charCount);
			byte aByte = buffer.getByte(buffer.length() - 1);
			buffer.setByte(buffer.length() - 1, (byte)(aByte | 0x80));
		} else {
			writeUtf8Length(charCount + 1);
			//continue here
			//
			//
			//
			//
		}
		
		
		
	}
	
	public void writeString(CharSequence value) {
		if(value == null) {
			writeByte(0x80);
			return;
		}
		int charCount = value.length();
		if(charCount == 0) {
			writeByte(1 | 0x80);
			return;
		}
		writeUtf8Length(charCount + 1);
		//continue here
		//
		//
		//
		//
	}
	
	public void writeAscii(String value) {
		//
	}
	
	
	public void writeAscii_slow(String value, int charCount) {
		//
	}
   
	public void writeUtf8Length(int value) {
		//
	}
	
   
   public static void main(String[] args) {
	   VertxBufferOutput v = new VertxBufferOutput(Buffer.buffer());
	   v.writeString("");
	   byte[] bytes = v.toBytes();
	   for(byte b : bytes) {
		   System.out.println(b);
	  }

   }
   
    
}
