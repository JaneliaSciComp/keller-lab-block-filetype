%test speed vs compression for different block sizes
function [results] = testOptimalBlockSize(im)

%main options (you can use default if you want to simplify)
numThreads = -1;
pixelSize = [];
compressionType = [];


if( isempty(im) )%generate random image
    imSize = [950 784 400];
    im = uint32( reshape( [1:prod(imSize)], imSize) );
else
    imSize = size(im);
end

%temp filename
basename = [tempname];

%number of bytes per pixel
qq = whos('im');
bytesPerPixel = qq.bytes / prod(imSize);

%results = nan(8*8*16,6);
results = nan(1*8*16,6);
filenameVec = cell(8*8*16,1);
count = 0;
for bx = linspace(8,64,8)
    for by = linspace(8,64,8)
        for bz = [8 16]
            
            blockSize = [bx by bz]
            
            
            
            tic;
            filenameB = [basename '_' num2str( blockSize(1) ) '_' num2str( blockSize(2) ) '_' num2str( blockSize(3) ) '.klb'];
            writeKLBstack(im, filenameB ,numThreads,pixelSize,blockSize,compressionType);
            tt = toc;
            s = dir(filenameB);
            s = s.bytes / 2^10;
            ttR = -1;
            
            
            
            count = count + 1;
            results(count,:) = [bx by bz tt s ttR];%time(secs) , size(in KB)                                           
            filenameVec{count} = filenameB;
           
        end        
        save('blockSizeResults\temp.mat','results');
    end
end

%test read (we have to do it separately otherwise windows crashes by trying
%to open the file too soon
for ii = 1:length(filenameVec)
    if( isempty(filenameVec{ii}) )
        continue;
    end
    
    [ii results(ii,1:3)]
    tic;
    aux = readKLBstack(filenameVec{ii}, numThreads);
    ttR = toc;
    
    results(ii,6) = ttR;
    
    delete(filenameVec{ii});
end

save('blockSizeResults\temp.mat','results');