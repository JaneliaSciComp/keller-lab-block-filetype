function testMultithreadScaling(im)

blockSize = [96 96 8];
maxNumThreads = 12;

ttWrite = zeros(maxNumThreads,2);
ttRead = ttWrite;

basename = tempname;

for ii = 1:maxNumThreads
    tic;
    writeKLBstack(im, [basename '.klb'],ii,[],blockSize,[]);
    ttWrite(ii,1) = toc;
end


for ii = 1:maxNumThreads
    tic;
    aa = readKLBstack([basename '.klb'],ii);
    ttRead(ii,1) = toc;
end


for ii = 1:maxNumThreads
    tic;
    writeJPEG2000stack(im, [basename '.jp2'],ii,0);
    ttWrite(ii,2)=toc;
end

for ii = 1:maxNumThreads
    tic;
    bb = readJPEG2000stack([basename '.jp2'],ii);
    ttRead(ii,2)=toc;
end

tic;
writeTifStack(im, [basename]);
tt = toc;
ttWrite(:,3) = tt;

tic;
cc = readTIFFstack([basename '.tif']);
tt = toc;
ttRead(:,3) = tt;





save('threadResults\tempMulthreadScale.mat','ttRead','ttWrite');

%%
set(0,'defaultAxesFontName', 'Arial')
set(0,'defaultTextFontName', 'Arial')

mm = numel(im) * 2 / 2^20;%image size in MB

%add theoretical
ttWrite(:,4) = ttWrite(1,1) ./ [1:maxNumThreads];
ttRead(:,4) = ttRead(1,1) ./ [1:maxNumThreads];

h1 = figure;
plot([1:maxNumThreads],mm./ttRead,'linewidth',2);
legend('KLB','JP2','Matlab Tif','KLB optimal', 'location','best');
xlabel('Number of threads');
ylabel('MB / secs');
xlim([1 maxNumThreads]);
title('Read image')
editFigure(h1, 24, 18, 18);

h2 = figure;
plot([1:maxNumThreads],mm./ttWrite,'linewidth',2);
legend('KLB','JP2','Matlab Tif', 'KLB optimal','location','best');
xlabel('Number of threads');
ylabel('MB / secs');
xlim([1 maxNumThreads]);
title('Write image')
editFigure(h2, 24, 18, 18);