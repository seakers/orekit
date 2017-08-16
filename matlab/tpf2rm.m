function [raan, ma] = tpf2rm(t,p,f)
%this function converts the tpf values from a walker constellation to the
%corresponding right ascension of the ascending node and the mean anomaly.

if (t < 0 || p < 0)
    error('Expected t>0, p>0. Found f=%d and p=%d', t, p);
elseif ( mod(t,p) ~= 0)
    error('Incompatible values for total number of satellites <t=%d> and number of planes <p=%d>. t must be divisible by p.', t, p);
elseif(f < 0 && f > p - 1)
    error('Expected 0 <= f <= p-1. Found f = %d and p = %d.', f, p);
end

%Uses Walker delta pa
s = t / p; %number of satellites per plane
pu = 2 * pi / t; %pattern unit
delAnom = pu * p; %in plane spacing between satellites
delRaan = pu * s; %node spacing
phasing = pu * f;

raan = zeros(t,1);
ma = zeros(t,1);

satCount = 1;
for planeNum = 1 : p
    for satNum = 1 : s
        raan(satCount) = planeNum * delRaan;
        ma(satCount) = satNum * delAnom + phasing * planeNum;
        satCount = satCount + 1;
    end
end
end