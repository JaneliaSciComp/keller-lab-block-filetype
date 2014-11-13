stackNames = {...
    'Dme_L1_UasGCaMP6s-57C10Gal4_0_20141015_160052.corrected.SPM00_TM000050_CM00_CHN00.jp2'; ...
    'Dre_L1_HuCGCaMP5_0_AIF2_20140626_230346.corrected.SPM00_TM000020_CM00_CHN00.jp2'; ...
    'Dme_E1_His2AvEGFP-DPN20xmCherry_01-23_20140714_165604.corrected.SPM00_TM000020_CM00_CM01_CHN00_CHN01.fusedStack.jp2'; ...
    'Mmu_E1_CAGTAG1_01_20140826_154247.corrected.SPM00_TM000330_CM00_CM01_CHN00.fusedStack.jp2'; ...
    % 'Mmu_E1_CAGTAG1_01_20140826_154247.corrected.SPM00_TM000330_CM00_CM01_CHN01.fusedStack.jp2'; ...
    };

inputFolder           = 'Stacks';
outputFolder          = 'Measurements';

lateralBlockSizes     = [32 48 64 96 128 192 256 384 512];
axialBlockSizes       = [1 2 4 6 8 10 12 14 16];

jpegWorkers           = 12;
compressionLevel      = 0;

threadCount           = -1;
pixelSize             = [];
compressionType       = [];

referenceIterations   = 5;
measurementIterations = 3;

if exist(outputFolder, 'dir') ~= 7
    mkdir(outputFolder);
end;

globalWriteTimeMatrix = zeros(numel(lateralBlockSizes), numel(axialBlockSizes));
globalReadTimeMatrix = zeros(numel(lateralBlockSizes), numel(axialBlockSizes));
globalSizeMatrix = zeros(numel(lateralBlockSizes), numel(axialBlockSizes));

for s = 1:numel(stackNames)
    currentName = [inputFolder '\' stackNames{s}];
    
    referenceWriteTime = 0;
    referenceReadTime = 0;
    
    writeTimeMatrix = zeros(numel(lateralBlockSizes), numel(axialBlockSizes));
    readTimeMatrix = zeros(numel(lateralBlockSizes), numel(axialBlockSizes));
    sizeMatrix = zeros(numel(lateralBlockSizes), numel(axialBlockSizes));
    
    stack = readJPEG2000stack(currentName, jpegWorkers);
    
    xSize = size(stack, 1);
    ySize = size(stack, 2);
    zSize = size(stack, 3);
    
    for r = 1:referenceIterations
        outputName = [outputFolder '\performanceAnalysisTempStack_' num2str(r) '.jp2'];
        
        tic;
        writeJPEG2000stack(stack, outputName, jpegWorkers, compressionLevel);
        referenceWriteTime = referenceWriteTime + toc;
        
        pause(0.5);
    end;
    
    referenceWriteTime = referenceWriteTime / referenceIterations;
    
    fileInformation = dir(outputName);
    referenceSize = fileInformation.bytes;
    
    for r = 1:referenceIterations
        outputName = [outputFolder '\performanceAnalysisTempStack_' num2str(r) '.jp2'];
        
        tic;
        tempStack = readJPEG2000stack(outputName, jpegWorkers);
        referenceReadTime = referenceReadTime + toc;
        
        pause(0.5);
        
        delete(outputName);
    end;
    
    referenceReadTime = referenceReadTime / referenceIterations;
    
    for l = 1:numel(lateralBlockSizes)
        for a = 1:numel(axialBlockSizes)
            blockSize = [lateralBlockSizes(l) lateralBlockSizes(l) axialBlockSizes(a)];
            
            for i = 1:measurementIterations
                outputName = [outputFolder '\performanceAnalysisTempStack_' num2str(blockSize(1)) '_' num2str(blockSize(2)) '_' num2str(blockSize(3)) '_' num2str(i) '.klb'];
                
                tic;
                writeKLBstack(stack, outputName, threadCount, pixelSize, blockSize, compressionType);
                writeTimeMatrix(l, a) = writeTimeMatrix(l, a) + toc / referenceWriteTime;
                
                pause(0.5);
            end;
            
            writeTimeMatrix(l, a) = writeTimeMatrix(l, a) / measurementIterations;
            
            fileInformation = dir(outputName);
            sizeMatrix(l, a) = fileInformation.bytes / referenceSize;
        end;
    end;
    
    for l = 1:numel(lateralBlockSizes)
        for a = 1:numel(axialBlockSizes)
            blockSize = [lateralBlockSizes(l) lateralBlockSizes(l) axialBlockSizes(a)];
            
            for i = 1:measurementIterations
                outputName = [outputFolder '\performanceAnalysisTempStack_' num2str(blockSize(1)) '_' num2str(blockSize(2)) '_' num2str(blockSize(3)) '_' num2str(i) '.klb'];
                
                tic;
                tempStack = readKLBstack(outputName, threadCount);
                readTimeMatrix(l, a) = readTimeMatrix(l, a) + toc / referenceReadTime;
                
                pause(0.5);
                
                delete(outputName);
            end;
            
            readTimeMatrix(l, a) = readTimeMatrix(l, a) / measurementIterations;
        end;
    end;
    
    outputDatabaseName = [outputFolder '\' stackNames{s}(1:(end - 3)) 'mat'];
    save(outputDatabaseName, 'referenceWriteTime', 'referenceReadTime', 'referenceSize', 'writeTimeMatrix', 'readTimeMatrix', 'sizeMatrix');
    
    figure;
    imagesc(writeTimeMatrix);
    title('KLB write time');
    xlabel('Axial block size');
    set(gca, 'XTick', 1:numel(axialBlockSizes));
    set(gca, 'XTickLabel', axialBlockSizes);
    ylabel('Lateral block size');
    set(gca, 'YTick', 1:numel(lateralBlockSizes));
    set(gca, 'YTickLabel', lateralBlockSizes);
    colormap([(0:(1/63):1)' (1:-(1/63):0)' (1:-(1/63):0)']);
    % colormap([(0:(1/63):1)' (1:-(1/63):0)' zeros(64, 1)]);
    % colormap([(0:(1/63):1)' zeros(64, 1) (1:-(1/63):0)']);
    c = colorbar;
    ylabel(c, 'KLB/JP2 ratio (lower is better)');
    h = gca;
    currentFile = [outputFolder '\' stackNames{s}(1:(end - 3)) 'writeTime.png'];
    saveas(h, currentFile);
    currentFrame = imread(currentFile);
    imwrite(currentFrame, currentFile, 'png');
    close;
    
    figure;
    imagesc(readTimeMatrix);
    title('KLB read time');
    xlabel('Axial block size');
    set(gca, 'XTick', 1:numel(axialBlockSizes));
    set(gca, 'XTickLabel', axialBlockSizes);
    ylabel('Lateral block size');
    set(gca, 'YTick', 1:numel(lateralBlockSizes));
    set(gca, 'YTickLabel', lateralBlockSizes);
    colormap([(0:(1/63):1)' (1:-(1/63):0)' (1:-(1/63):0)']);
    % colormap([(0:(1/63):1)' (1:-(1/63):0)' zeros(64, 1)]);
    % colormap([(0:(1/63):1)' zeros(64, 1) (1:-(1/63):0)']);
    c = colorbar;
    ylabel(c, 'KLB/JP2 ratio (lower is better)');
    h = gca;
    currentFile = [outputFolder '\' stackNames{s}(1:(end - 3)) 'readTime.png'];
    saveas(h, currentFile);
    currentFrame = imread(currentFile);
    imwrite(currentFrame, currentFile, 'png');
    close;
    
    figure;
    imagesc(sizeMatrix);
    title('KLB file size');
    xlabel('Axial block size');
    set(gca, 'XTick', 1:numel(axialBlockSizes));
    set(gca, 'XTickLabel', axialBlockSizes);
    ylabel('Lateral block size');
    set(gca, 'YTick', 1:numel(lateralBlockSizes));
    set(gca, 'YTickLabel', lateralBlockSizes);
    colormap([(0:(1/63):1)' (1:-(1/63):0)' (1:-(1/63):0)']);
    % colormap([(0:(1/63):1)' (1:-(1/63):0)' zeros(64, 1)]);
    % colormap([(0:(1/63):1)' zeros(64, 1) (1:-(1/63):0)']);
    c = colorbar;
    ylabel(c, 'KLB/JP2 ratio (lower is better)');
    h = gca;
    currentFile = [outputFolder '\' stackNames{s}(1:(end - 3)) 'fileSize.png'];
    saveas(h, currentFile);
    currentFrame = imread(currentFile);
    imwrite(currentFrame, currentFile, 'png');
    close;
    
    globalWriteTimeMatrix = globalWriteTimeMatrix + writeTimeMatrix;
    globalReadTimeMatrix = globalReadTimeMatrix + readTimeMatrix;
    globalSizeMatrix = globalSizeMatrix + sizeMatrix;
end;

globalWriteTimeMatrix = globalWriteTimeMatrix ./ numel(stackNames);
globalReadTimeMatrix = globalReadTimeMatrix ./ numel(stackNames);
globalSizeMatrix = globalSizeMatrix ./ numel(stackNames);

globalAverageMatrix = (globalWriteTimeMatrix + globalReadTimeMatrix + globalSizeMatrix) ./ 3;

outputDatabaseName = [outputFolder '\averagePerformance.mat'];
save(outputDatabaseName, 'globalWriteTimeMatrix', 'globalReadTimeMatrix', 'globalSizeMatrix', 'globalAverageMatrix');

figure;
imagesc(globalWriteTimeMatrix);
title('KLB write time');
xlabel('Axial block size');
set(gca, 'XTick', 1:numel(axialBlockSizes));
set(gca, 'XTickLabel', axialBlockSizes);
ylabel('Lateral block size');
set(gca, 'YTick', 1:numel(lateralBlockSizes));
set(gca, 'YTickLabel', lateralBlockSizes);
colormap([(0:(1/63):1)' (1:-(1/63):0)' (1:-(1/63):0)']);
% colormap([(0:(1/63):1)' (1:-(1/63):0)' zeros(64, 1)]);
% colormap([(0:(1/63):1)' zeros(64, 1) (1:-(1/63):0)']);
c = colorbar;
ylabel(c, 'KLB/JP2 ratio (lower is better)');
h = gca;
currentFile = [outputFolder '\averagePerformance.writeTime.png'];
saveas(h, currentFile);
currentFrame = imread(currentFile);
imwrite(currentFrame, currentFile, 'png');
close;

figure;
imagesc(globalReadTimeMatrix);
title('KLB read time');
xlabel('Axial block size');
set(gca, 'XTick', 1:numel(axialBlockSizes));
set(gca, 'XTickLabel', axialBlockSizes);
ylabel('Lateral block size');
set(gca, 'YTick', 1:numel(lateralBlockSizes));
set(gca, 'YTickLabel', lateralBlockSizes);
colormap([(0:(1/63):1)' (1:-(1/63):0)' (1:-(1/63):0)']);
% colormap([(0:(1/63):1)' (1:-(1/63):0)' zeros(64, 1)]);
% colormap([(0:(1/63):1)' zeros(64, 1) (1:-(1/63):0)']);
c = colorbar;
ylabel(c, 'KLB/JP2 ratio (lower is better)');
h = gca;
currentFile = [outputFolder '\averagePerformance.readTime.png'];
saveas(h, currentFile);
currentFrame = imread(currentFile);
imwrite(currentFrame, currentFile, 'png');
close;

figure;
imagesc(globalSizeMatrix);
title('KLB file size');
xlabel('Axial block size');
set(gca, 'XTick', 1:numel(axialBlockSizes));
set(gca, 'XTickLabel', axialBlockSizes);
ylabel('Lateral block size');
set(gca, 'YTick', 1:numel(lateralBlockSizes));
set(gca, 'YTickLabel', lateralBlockSizes);
colormap([(0:(1/63):1)' (1:-(1/63):0)' (1:-(1/63):0)']);
% colormap([(0:(1/63):1)' (1:-(1/63):0)' zeros(64, 1)]);
% colormap([(0:(1/63):1)' zeros(64, 1) (1:-(1/63):0)']);
c = colorbar;
ylabel(c, 'KLB/JP2 ratio (lower is better)');
h = gca;
currentFile = [outputFolder '\averagePerformance.fileSize.png'];
saveas(h, currentFile);
currentFrame = imread(currentFile);
imwrite(currentFrame, currentFile, 'png');
close;

figure;
imagesc(globalAverageMatrix);
title('KLB average performance');
xlabel('Axial block size');
set(gca, 'XTick', 1:numel(axialBlockSizes));
set(gca, 'XTickLabel', axialBlockSizes);
ylabel('Lateral block size');
set(gca, 'YTick', 1:numel(lateralBlockSizes));
set(gca, 'YTickLabel', lateralBlockSizes);
colormap([(0:(1/63):1)' (1:-(1/63):0)' (1:-(1/63):0)']);
% colormap([(0:(1/63):1)' (1:-(1/63):0)' zeros(64, 1)]);
% colormap([(0:(1/63):1)' zeros(64, 1) (1:-(1/63):0)']);
c = colorbar;
ylabel(c, 'KLB/JP2 ratio (lower is better)');
h = gca;
currentFile = [outputFolder '\averagePerformance.globalMean.png'];
saveas(h, currentFile);
currentFrame = imread(currentFile);
imwrite(currentFrame, currentFile, 'png');
close;