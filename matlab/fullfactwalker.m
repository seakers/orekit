function constels = fullfactwalker(alts, incs, t)
%this function will enumerate all possible walker constellations given a
%discrete set of altitudes, inclinations, and total number of satellites.
%By defining the total number of satellites, all possible values of planes
%and phasing values will be created such that walker delta-pattern
%constraints are imposed.
%Input Parameter Units:
%altitude [km]
%inc [deg]
%t integer value
%
%the resulting constels matrix will be a matrix where the rows are
%different walker constellation configurations with the columns ordered
%left to right as altitude, inclination, total number of satellites, number
%of planes, and phasing value.
%Output Parameter Units:
%altitude [km]
%inc [deg]
%t integer value
%p integer value
%f integer value

%first check for valid values for t. Make sure they are positive integers
if sum(t<0)>1
    error('Expected all t values to be positive. Found at least one instance of a negative number.')
end
for i = 1:length(t)
    if rem(t(i),1) ~= 0
        error('Expected an integer for divisors(n). Found %d.', t(i));
    end
end


%loop over all possible values for each parameter and store them in
%constels

resizeStep = 10000;
constel_count = 0;
constels = zeros(resizeStep, 5);
for i_a = 1:length(alts)
    for i_i = 1:length(incs)
        for i_t = 1:length(t)
            planes = divisors(t(i_t));
            for i_p = 1:length(planes)
                for i_f = 0:planes(i_p)-1
                    constel_count = constel_count + 1;
                    %check if constel needs to be resized
                    if constel_count > size(constels,1)
                        constels(constel_count + resizeStep,:) = 0;
                    end
                    
                    constels(constel_count,:) = [alts(i_a), incs(i_i), ...
                        t(i_t), planes(i_p), i_f];
                end
            end
        end
    end
end

constels = constels(1:constel_count,:);

end




function divs = divisors(n)
%this function finds the divisors to n. Returns an array of all the
%divisors

facts = factor(n);
prime = unique(facts);
%count the number of times a prime number reoccurs
count = zeros(1, length(prime));
for i=1:length(prime)
    count(i) = sum(facts==prime(i));
end

combos = fullfact(count+1)-1;
divs = zeros(size(combos,1),1);
for i=1:size(combos,1)
    divs(i) = prod(prime.^combos(i,:));
end

end
