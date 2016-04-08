#!/usr/bin/perl -w

$vj = "astex/Version.java";

if(-e $vj){
  open(VERSION, "< $vj") or
    die("couldn't open version file $vj ($!)");

  while(<VERSION>){
    @f = split;

    if(/private static int/){
      if(/minor/){
	$minor = $f[5];
      }elsif(/major/){
	$major = $f[5];
      }elsif(/build/){
	$build = $f[5];
      }
    }
  }

  close VERSION;

}else{
  $major = 1;
  $minor = 0;
  $build = 0;
}

$build++;

print "version ${major}.${minor}.${build}\n";

open(VERSION, "> $vj") or
    die("couldn't open version file $vj ($!)");

print VERSION << "EOF";
package astex;

public class Version {
  private static int major = $major ;
  private static int minor = $minor ;
  private static int build = $build ;

  public static String getVersion(){
    return major + "." + minor + "." + build;
  }
}
EOF

close VERSION
