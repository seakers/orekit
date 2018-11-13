package seakers.orekit.util;

import org.hipparchus.util.FastMath;
import org.orekit.utils.Constants;

public class Radar {

    public static final double c = 3e8;

    public static final double kb=1.38e-23;

    public static double f2l(double f){
        return c/f;
    }

    public static double rangeResolution(double bandwidth, double theta){
        return c/2/bandwidth/ FastMath.sin(FastMath.toRadians(theta));
    }

    public static double azimuthResolution(double h, double f, double L, double theta){
        return h*f2l(f)/L/FastMath.cos(FastMath.toRadians(theta));
    }

    public static double azimuthResolutionFocusedSAR(double L){
        return L/2;
    }

    public static double azimuthResolutionUnfocusedSAR(double f, double h){
        return FastMath.sqrt(2*h*f2l(f));
    }

    public static double[] sizeRadar(double rangeResolution, double azimuthResolution, double h, double f, double theta){
        double B=c/2/rangeResolution/FastMath.sin(FastMath.toRadians(theta));
        double L=h*f2l(f)/FastMath.cos(FastMath.toRadians(theta))/azimuthResolution;
        double[] radar={B,L};
        return radar;
    }

    public static double[] sizeSAR(double rangeResolution, double azimuthResolution, double theta){
        double B=c/2/rangeResolution/FastMath.sin(FastMath.toRadians(theta));
        double L=2*azimuthResolution;
        double[] SAR={B,L};
        return SAR;
    }

    public static double SNRRadar(double Pt, double L, double W, double h, double f, double B, double T, double theta,
                                  double sigmaDB, double hrz_avg, SARType SARType, ApertureType apertureType){
        double Aeff=0;
        if (apertureType.equals(ApertureType.RECTANGULAR)){
            double eff=0.55;
            Aeff=eff*L*W;
        } else if(apertureType.equals(ApertureType.CIRCULAR)){
            double eff=0.55;
            Aeff=eff*FastMath.PI*FastMath.pow(L,2)/4;
        } else {
            throw new IllegalArgumentException("Aperture type should be either circular or rectangular");
        }
        double G = 4*FastMath.PI*Aeff/FastMath.pow(f2l(f),2);
        double Xr= rangeResolution(B,theta);
        double Xa=0;
        switch (SARType){
            case UNFOCUSED:
                Xa = azimuthResolutionUnfocusedSAR(f,h);
            case FOCUSED:
                Xa = azimuthResolutionFocusedSAR(L);
            case REGULAR:
                Xa = azimuthResolution(h,f,L,theta);
        }
        double Nr;
        double Na;
        if (hrz_avg>0) {
            Nr = hrz_avg / Xr;
            if (Nr< 1) {
                Nr = 1;
            }
            Na = hrz_avg / Xa;
            if (Na< 1) {
                Na = 1;
            }
        }else {
            Na = 1;
            Nr = 1;
        }
        double area = Na*Xa*Nr*Xr*FastMath.cos(FastMath.toRadians(theta))*FastMath.pow(10 , sigmaDB/10);
        double r = h/FastMath.cos(FastMath.toRadians(theta));
        double S=(Pt*G/4/FastMath.PI/FastMath.pow(r,2))*area*(Aeff/4/FastMath.PI/FastMath.pow(r,2));
        return S/(kb*T*B);
    }

    public static double SNR_SAR(double Pt, double L, double W, double h, double f, double B, double T, double theta,
                                  double sigmaDB, double tau, double PRF, SARType SARType, ApertureType apertureType){
        double Aeff=0;
        if (apertureType.equals(ApertureType.RECTANGULAR)){
            double eff=0.55;
            Aeff=eff*L*W;
        } else if(apertureType.equals(ApertureType.CIRCULAR)){
            double eff=0.55;
            Aeff=eff*FastMath.PI*FastMath.pow(L,2)/4;
        } else {
            throw new IllegalArgumentException("Aperture type should be either circular or rectangular");
        }
        double G = 4*FastMath.PI*Aeff/FastMath.pow(f2l(f),2);
        double Xr= rangeResolution(B,theta);
        double Xa=0;
        switch (SARType){
            case UNFOCUSED:
                Xa = azimuthResolutionUnfocusedSAR(f,h);
            case FOCUSED:
                Xa = azimuthResolutionFocusedSAR(L);
            case REGULAR:
                Xa = azimuthResolution(h,f,L,theta);
        }

        double Gr=tau*B;
        double v = Orbits.circularOrbitVelocity(h + Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
        double Ga=PRF*f2l(f)*h*1000/Xa/v;
        double area = Gr*Ga*Xa*Xr*FastMath.cos(FastMath.toRadians(theta))*FastMath.pow(10 , sigmaDB/10);
        double r = h/FastMath.cos(FastMath.toRadians(theta));
        double S=(Pt*G/4/FastMath.PI/FastMath.pow(r,2))*area*(Aeff/4/FastMath.PI/FastMath.pow(r,2));
        return S/(kb*T*B);
    }

    public static double noiseEquivalentSigma(double Pt, double L, double W, double h, double f, double B, double T, double theta,
                                              double sigmaDB, double hrz_avg, SARType SARType, ApertureType apertureType){
        double SNR = SNRRadar(Pt,L,W,h,f,B,T,theta,sigmaDB,hrz_avg,SARType,apertureType);
        return 10*FastMath.log(10,1/SNR);
    }

    public enum ApertureType{
        RECTANGULAR,
        CIRCULAR
    }

    public enum SARType{
        FOCUSED,
        UNFOCUSED,
        REGULAR
    }

}
