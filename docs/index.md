JVARKIT
=======

Author      : Pierre Lindenbaum Phd. Institut du Thorax. Nantes. France.
Version     : 8e5f425de
Compilation : 20230331121218
Github      : https://github.com/lindenb/jvarkit
Issues      : https://github.com/lindenb/jvarkit/issues

## Usage

```
  java -jar jvarkit.jar [options]
```
or
```
  java -jar jvarkit.jar <command name> (other arguments)
```

## Options

 + --help show this screen
 + --help-all show all commands, including the private ones.
 + --version print version

## Compilation Installation

Please, read [how to run and install jvarkit](JvarkitCentral.md)

## Tools

### BAM Visualization

| Tool | Description | Creation | Update |
| ---: | :---------- | :------: | :----: |
| [bam2raster](Bam2Raster.md) | BAM to raster graphics |  |  |
| [bam2svg](BamToSVG.md) | BAM to Scalar Vector Graphics (SVG) | 20141013 | 20210728 |
| [biostar139647](Biostar139647.md) | Convert alignment in Fasta/Clustal format to SAM/BAM file |  |  |
| [biostar145820](Biostar145820.md) | subsample/shuffle BAM to fixed number of alignments. | 20150615 | 20211005 |
| [lowresbam2raster](LowResBam2Raster.md) | Low Resolution BAM to raster graphics | 20170523 | 20211126 |
| [mkminibam](MakeMiniBam.md) | Creates an archive of small bams with only a few regions. | 20190410 | 20221019 |
| [plotsashimi](PlotSashimi.md) | Print Sashimi plots from Bam | 20191117 | 20191104 |
| [prettysam](PrettySam.md) | Pretty SAM alignments | 20171215 | 20211105 |
| [wgscoverageplotter](WGSCoveragePlotter.md) | Whole genome coverage plotter | 20201125 | 20210812 |

### CNV/SV

| Tool | Description | Creation | Update |
| ---: | :---------- | :------: | :----: |
| [bammatrix](BamMatrix.md) | Bam matrix, inspired from 10x/loupe | 20190620 | 20211206 |
| [cnvtview](CnvTView.md) | Text visualization of bam DEPTH for multiple regions in a terminal | 20181018 | 20210412 |
| [coverageplotter](CoveragePlotter.md) | Display an image of depth to display any anomaly an intervals+bams | 20200605 | 20221125 |
| [samfindclippedregions](SamFindClippedRegions.md) | Fins clipped position in one or more bam. | 20140228 | 20220329 |
| [swingindexcov](SwingIndexCov.md) | indexcov visualization | 2020511 | 2020512 |
| [vcfstrech2svg](VcfStrechToSvg.md) | another VCF to SVG | 20210304 | 20210309 |
| [wescnvsvg](WesCnvSvg.md) | SVG visualization of bam DEPTH for multiple regions | 20180726 | 20210726 |

### Functional prediction

| Tool | Description | Creation | Update |
| ---: | :---------- | :------: | :----: |
| [backlocate](BackLocate.md) | Mapping a mutation on a protein back to the genome. | 20140619 | 20190820 |
| [groupbygene](GroupByGene.md) | Group VCF data by gene/transcript. By default it uses data from VEP , SnpEff | 20131209 | 20220529 |

### BED Manipulation

| Tool | Description | Creation | Update |
| ---: | :---------- | :------: | :----: |
| [bedcluster](BedCluster.md) | Clusters a BED file into a set of BED files. | 20200130 | 20220914 |
| [bedmergecnv](BedMergeCnv.md) | Merge continuous sorted bed records if they overlap a fraction of their lengths. | 20200330 | 20200603 |
| [bednonoverlappingset](BedNonOverlappingSet.md) | Split a Bed file into non-overlapping data set. | 20180607 | 20200408 |
| [bedrenamechr](ConvertBedChromosomes.md) | Convert the names of the chromosomes in a Bed file |  | 20190503 |
| [setfiletools](SetFileTools.md) | Utilities for the setfile format | 20210125 | 20220426 |

### Biostars

| Tool | Description | Creation | Update |
| ---: | :---------- | :------: | :----: |
| [biostar103303](Biostar103303.md) | Calculate Percent Spliced In (PSI). |  |  |
| [biostar105754](Biostar105754.md) | bigwig : peak distance from specific genomic region | 20140708 | 20220110 |
| [biostar165777](Biostar165777.md) | Split a XML file | 20151114 | 20151114 |
| [biostar170742](Biostar170742.md) | convert sam format to axt Format | 20151228 | 20210412 |
| [biostar172515](Biostar172515.md) | Convert BAI to XML |  |  |
| [biostar173114](Biostar173114.md) | make a bam file smaller by removing unwanted information see also https://www.biostars.org/p/173114/ |  |  |
| [biostar175929](Biostar175929.md) | Construct a combination set of fasta sequences from a vcf | 20160208 | 20211012 |
| [biostar178713](Biostar178713.md) | split bed file into several bed files where each region is separated of any other by N bases | 20160226 | 20200818 |
| [biostar214299](Biostar214299.md) | Extract allele specific reads from bamfiles | 20160930 | 20220420 |
| [biostar234081](Biostar234081.md) | convert extended CIGAR to regular CIGAR ('X','=' -> 'M') | 20170130 | 20200409 |
| [biostar234230](Biostar234230.md) | Sliding Window : discriminate partial and fully contained fragments (from a bam file) |  | 20190417 |
| [biostar251649](Biostar251649.md) | Annotating the flanking bases of SNPs in a VCF file | 20170508 | 20200213 |
| [biostar322664](Biostar322664.md) | Extract PE Reads (with their mates) supporting variants in vcf file |  |  |
| [biostar332826](Biostar332826.md) | Fast Extraction of Variants from a list of IDs | 20180817 | 20210412 |
| [biostar336589](Biostar336589.md) | displays circular map as SVG from BED and REF file | 20180907 | 20210818 |
| [biostar352930](Biostar352930.md) | Fills the empty SEQ(*) and QUAL(*) in a bam file using the the reads with the same name carrying this information. |  |  |
| [biostar398854](Biostar398854.md) | Extract every CDS sequences from a VCF file | 20190916 | 20190916 |
| [biostar404363](Biostar404363.md) | introduce artificial mutation SNV in bam | 20191023 | 20191024 |
| [biostar480685](Biostar480685.md) | paired-end bam clip bases outside insert range | 20201223 | 20200220 |
| [biostar489074](Biostar489074.md) | call variants for every paired overlaping read | 20200205 | 20210412 |
| [biostar497922](Biostar497922.md) | Split VCF into separate VCFs by SNP count | 20210319 | 20210319 |
| [biostar59647](Biostar59647.md) | SAM/BAM to XML | 20131112 | 20190926 |
| [biostar76892](Biostar76892.md) | fix strand of two paired reads close but on the same strand. |  |  |
| [biostar77288](Biostar77288.md) | Low resolution sequence alignment visualization |  |  |
| [biostar77828](Biostar77828.md) | Divide the human genome among X cores, taking into account gaps |  |  |
| [biostar78285](Biostar78285.md) | Extract BAMs coverage as a VCF file. |  |  |
| [biostar81455](Biostar81455.md) | Defining precisely the exonic genomic context based on a position . | 20130918 | 20200603 |
| [biostar84452](Biostar84452.md) | remove clipped bases from a BAM file |  |  |
| [biostar84786](Biostar84786.md) | Matrix transposition |  |  |
| [biostar86363](Biostar86363.md) | Set genotype of specific sample/genotype comb to unknown in multisample vcf file. See http://www.biostars.org/p/86363/ |  |  |
| [biostar86480](Biostar86480.md) | Genomic restriction finder | 20131114 | 20220426 |
| [biostar90204](Biostar90204.md) | Bam version of linux split. |  |  |
| [biostar9462889](Biostar9462889.md) | Extracting reads from a regular expression in a bam file | 20210402 | 20210402 |
| [biostar9469733](Biostar9469733.md) | Extract reads mapped within chosen intronic region from BAM file | 20210511 | 20210511 |
| [biostar9501110](Biostar9501110.md) | Keep reads including/excluding variants from VCF | 20211210 | 20211213 |
| [biostar9556602](Biostar9556602.md) | Filtering of tricky overlapping sites in VCF |  |  |

### Deprecated/barely used

| Tool | Description | Creation | Update |
| ---: | :---------- | :------: | :----: |
| [addlinearindextobed](AddLinearIndexToBed.md) | Use a Sequence dictionary to create a linear index for a BED file. Can be used as a X-Axis for a chart. | 20140201 | 20230126 |
| [bam2sql](BamToSql.md) | Convert a SAM/BAM to sqlite statements | 20160414 | 20160414 |
| [bam2xml](Bam2Xml.md) | converts a BAM to XML | 20130506 | 20210315 |

### Pubmed

| Tool | Description | Creation | Update |
| ---: | :---------- | :------: | :----: |
| [pubmed404](Pubmed404.md) | Test if URL in the pubmed abstracts are reacheable. | 20181210 | 20200204 |
| [pubmedcodinglang](PubmedCodingLanguages.md) | Programming language use distribution from recent programs / articles | 20170404 | 20200223 |
| [pubmeddump](PubmedDump.md) | Dump XML results from pubmed/Eutils | 20140805 | 20200204 |
| [pubmedgender](PubmedGender.md) | Add gender-related attributes in the Author tag of pubmed xml. |  |  |
| [pubmedgraph](PubmedGraph.md) | Creates a Gephi-gexf graph of references-cites for a given PMID | 20150605 | 20200220 |

### GTF/GFF Manipulation

| Tool | Description | Creation | Update |
| ---: | :---------- | :------: | :----: |
| [gtf2bed](GtfToBed.md) | Convert GTF/GFF3 to BED. | 20220629 | 20220630 |
| [gtf2xml](Gtf2Xml.md) | Convert GTF/GFF to XML | 20150811 | 20190823 |

### Utilities

| Tool | Description | Creation | Update |
| ---: | :---------- | :------: | :----: |
| [goutils](GoUtils.md) | Gene Ontology Utils. Retrieves terms from Gene Ontology | 20180130 | 20211020 |
| [ncbitaxonomy2xml](NcbiTaxonomyToXml.md) | Dump NCBI taxonomy tree as a hierarchical XML document |  |  |
| [oboutils](OboUtils.md) | OBO Ontology Utils. | 20230105 | 20230105 |
| [samplesrdf](SamplesRDF.md) | Digests a  database of samples from a set of recfiles | 20230201 | 20230202 |
| [ukbiobanksamples](UKBiobankSelectSamples.md) | Select samples from ukbiobank | 20210705 | 20220322 |
| [uniprot2svg](UniprotToSvg.md) | plot uniprot to SVG | 20220608 | 20220922 |
| [xsltstream](XsltStream.md) | XSLT transformation for large XML files. xslt is only applied on a given subset of nodes. |  | 20190222 |

### Unclassfied

| Tool | Description | Creation | Update |
| ---: | :---------- | :------: | :----: |
| [coverageserver](CoverageServer.md) | Jetty Based http server serving Bam coverage. | 20200212 | 20200330 |
| [evadumpfiles](EVADumpFiles.md) | Dump files locations from European Variation Archive | 20230314 | 20230314 |
| [gtexrs2qtl](GtexRsToQTL.md) | extract gtex eqtl data from a list of RS | 20230215 | 20230215 |
| [illuminadir](IlluminaDirectory.md) | Create a structured (**JSON** or **XML**) representation of a directory containing some Illumina FASTQs. | 20131021 | 20180717 |
| [kg2bed](KnownGenesToBed.md) | converts UCSC knownGenes file to BED. | 20140311 | 20190427 |
| [pubmedmap](PubmedMap.md) | Use Pubmed Author's Affiliation to map the authors in the world. | 20160426 |  |
| [sam2json](SamToJson.md) | Convert a SAM input to JSON | 20210402 | 20210315 |
| [sam4weblogo](SAM4WebLogo.md) | Sequence logo for different alleles or generated from SAM/BAM | 20130524 | 20191014 |
| [samjdk](SamJdk.md) | Filters a BAM using a java expression compiled in memory. | 20170807 | 20191119 |
| [vcfserver](VcfServer.md) | Web Server displaying VCF file. A web interface for vcf2table | 20171027 | 20220517 |
| [vcfspliceai](VcfSpliceAI.md) | Annotate VCF with spiceai web service | 20201107 | 20201107 |
| [vcftbi2bed](VcfTbiToBed.md) | extracts BED for each contig in a tabix-indexed VCF peeking first of last variant for each chromosome. | 20230214 | 20230214 |

### VCF Manipulation

| Tool | Description | Creation | Update |
| ---: | :---------- | :------: | :----: |
| [bioalcidaejdk](BioAlcidaeJdk.md) | java-based version of awk for bioinformatics | 20170712 | 20210412 |
| [biostar130456](Biostar130456.md) | Split individual VCF files from multisamples VCF file | 20150210 | 20200603 |
| [builddbsnp](BuildDbsnp.md) | Build a DBSNP file from different sources for GATK | 20200904 | 2021070726 |
| [findavariation](FindAVariation.md) | Finds a specific mutation in a list of VCF files | 20140623 | 20200217 |
| [findgvcfsblocks](FindGVCFsBlocks.md) | Find common blocks of calleable regions from a set of gvcfs | 20210806 | 20220401 |
| [minicaller](MiniCaller.md) | Simple and Stupid Variant Caller designed for @AdrienLeger2 | 201500306 | 20220705 |
| [swingvcfjexl](SwingVcfJexlFilter.md) | Filter VCF using Java Swing UI and JEXL/Javascript expression | 20220413 | 20220414 |
| [swingvcfview](SwingVcfView.md) | VCFviewer using Java Swing UI | 20210503 | 20210503 |
| [vcf2table](VcfToTable.md) | convert a vcf to a table, to ease display in the terminal | 20170511 | 20220507 |
| [vcfallelebalance](VcfAlleleBalance.md) | Insert missing allele balance annotation using FORMAT:AD | 20180829 | 20200805 |
| [vcfbigbed](VcfBigBed.md) | Annotate a VCF with values from a bigbed file | 20220107 | 20220107 |
| [vcfbigwig](VCFBigWig.md) | Annotate a VCF with values from a bigwig file | 20200506 | 20220110 |
| [vcfcadd](VcfCadd.md) | Annotate VCF with  Combined Annotation Dependent Depletion (CADD) (Kircher & al. A general framework for estimating the relative pathogenicity of human genetic variants. Nat Genet. 2014 Feb 2. doi: 10.1038/ng.2892.PubMed PMID: 24487276. | 20140218 | 20220119 |
| [vcfcombinetwosnvs](VCFCombineTwoSnvs.md) | Detect Mutations than are the consequences of two distinct variants. This kind of variant might be ignored/skipped from classical variant consequence predictor. Idea from @SolenaLS and then @AntoineRimbert | 20160215 | 20200425 |
| [vcffilterjdk](VcfFilterJdk.md) | Filtering VCF with dynamically-compiled java expressions | 20170705 | 20220830 |
| [vcffilterso](VcfFilterSequenceOntology.md) | Filter a VCF file annotated with SNPEff or VEP with terms from Sequence-Ontology. Reasoning : Children of user's SO-terms will be also used. | 20170331 | 20200924 |
| [vcfflatten](VCFFlatten.md) | Flatten variants to one variant | 20230222 | 20230222 |
| [vcfgenesplitter](VcfGeneSplitter.md) | Split VCF+VEP by gene/transcript. | 20160310 | 202220531 |
| [vcfgnomad](VcfGnomad.md) | Peek annotations from gnomad | 20170407 | 20200702 |
| [vcfhead](VcfHead.md) | print the first variants of a vcf | 20131210 | 20200518 |
| [vcfpar](VcfPseudoAutosomalRegion.md) | Flag human sexual regions excluding PAR. | 20200908 | 20200908 |
| [vcfpeekaf](VcfPeekAf.md) | Peek the AF from another VCF | 20200624 | 20200904 |
| [vcfphased01](VcfPhased01.md) | X10 Phased SVG to Scalar Vector Graphics (SVG) | 20190710 | 20190711 |
| [vcfpolyx](VCFPolyX.md) | Number of repeated REF bases around POS. | 20200930 | 20211102 |
| [vcfrebase](VcfRebase.md) | Restriction sites overlaping variations in a vcf | 20131115 | 20200624 |
| [vcfsetdict](VcfSetSequenceDictionary.md) | Set the `##contig` lines in a VCF header on the fly | 20140105 | 20210201 |
| [vcfshuffle](VCFShuffle.md) | Shuffle a VCF | 20131210 | 20200818 |
| [vcfsplitnvariants](VcfSplitNVariants.md) | Split VCF to 'N' VCF files | 202221122 | 202221201 |
| [vcftail](VcfTail.md) | print the last variants of a vcf | 20131210 | 20200518 |
| [vcftrio](VCFTrios.md) | Find mendelian incompatibilitie / denovo variants in a VCF | 20130705 | 20200624 |

### Retrocopy

| Tool | Description | Creation | Update |
| ---: | :---------- | :------: | :----: |
| [scanretrocopy](ScanRetroCopy.md) | Scan BAM for retrocopies | 20190125 | 20190709 |
| [starretrocopy](StarRetroCopy.md) | Scan retrocopies from the star-aligner/bwa output | 20190710 | 20191008 |

### BAM Manipulation

| Tool | Description | Creation | Update |
| ---: | :---------- | :------: | :----: |
| [bam2haplotypes](BamToHaplotypes.md) | Reconstruct SNP haplotypes from reads | 20211015 | 20211020 |
| [bamphased01](BamPhased01.md) | Extract Reads from a SAM/BAM file supporting at least two variants in a VCF file. | 20210218 | 20210218 |
| [bamrenamechr](ConvertBamChromosomes.md) | Convert the names of the chromosomes in a BAM file | 20131217 | 20191210 |
| [bamstats05](BamStats05.md) | Coverage statistics for a BED file, group by gene | 20151012 | 20210317 |
| [bamwithoutbai](BamWithoutBai.md) | Query a Remote BAM without bai | 20191213 | 20191217 |
| [basecoverage](BaseCoverage.md) | 'Depth of Coverage' per base. | 20220420 | 20220420 |
| [bioalcidaejdk](BioAlcidaeJdk.md) | java-based version of awk for bioinformatics | 20170712 | 20210412 |
| [biostar154220](Biostar154220.md) | Cap BAM to a given coverage | 20150812 | 20210312 |
| [findallcoverageatposition](FindAllCoverageAtPosition.md) | Find depth at specific position in a list of BAM files. My colleague Estelle asked: in all the BAM we sequenced, can you give me the depth at a given position ? | 20141128 | 20210818 |
| [sam2tsv](Sam2Tsv.md) | Prints the SAM alignments as a TAB delimited file. | 20170712 | 20210304 |
| [samgrep](SamGrep.md) | grep read-names in a bam file | 20130506 | 20210726 |
| [samrmdupnames](SamRemoveDuplicatedNames.md) | remove duplicated names in sorted BAM | 20221207 | 20221207 |
| [samviewwithmate](SamViewWithMate.md) | Extract reads within given region(s), and their mates | 20190207 | 20191004 |
| [sortsamrefname](SortSamRefName.md) | Sort a BAM on chromosome/contig and then on read/querty name | 20150812 | 20210312 |
| [swingbamcov](SwingBamCov.md) | Bam coverage viewer using Java Swing UI | 20210420 | 20220513 |
| [swingbamview](SwingBamView.md) | Read viewer using Java Swing UI | 20220503 | 20230331 |
| [texbam](TextBam.md) | Write text in a bam. Mostly for fun... | 20220708 | 20220708 |

