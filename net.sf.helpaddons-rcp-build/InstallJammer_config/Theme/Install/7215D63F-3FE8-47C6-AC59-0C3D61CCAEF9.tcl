proc CreateWindow.7215D63F-3FE8-47C6-AC59-0C3D61CCAEF9 {wizard id} {
    CreateWindow.CustomBlankPane1 $wizard $id
    set base [$wizard widget get $id]

    grid rowconfigure    $base 1 -weight 0
    grid rowconfigure    $base 5 -weight 1
    grid columnconfigure $base 1 -weight 1

    grid $base.image -row 0 -column 0 -rowspan 6 -sticky nw

    Label $base.title2 -height 3 -bg #FFFFFF -font TkCaptionFont -autowrap 1 -anchor w -justify left
    grid $base.title2 -row 0 -column 1 -sticky ew -padx 20 -pady [list 20 10]
    $id widget set Caption -type text -widget $base.title2

#    Label $base.message2 -bg #FF00FF -autowrap 1 -anchor nw -justify left
#    grid  $base.message2 -row 1 -column 1 -sticky news -padx 20 -pady [list 0 0]
#    $id widget set Message -type text -widget $base.message2
    grid $base.message -row 1 -column 1 -sticky nwe -padx 20 -pady [list 0 20]

    # See also section
    set changelog [::InstallAPI::SubstVirtualText -virtualtext "<%Changelog%>"]
    set urltext [::InstallAPI::SubstVirtualText -virtualtext "    <%WebSite%>"]
    
    if {[string trim $changelog] ne "" || [string trim $urltext] ne ""} {
        Label $base.seealso -text "See also:" -autowrap 1 -anchor nw -justify left -bg #FFFFFF
        grid $base.seealso -row 2 -column 1 -sticky nwe -padx 20 -pady 0
    }

    if {[string trim $changelog] ne ""} {
        Label $base.cl -foreground "blue" -text "    Changelog" -autowrap 1 -anchor nw -justify left -bg #FFFFFF
        grid $base.cl -row 3 -column 1 -sticky nwe -padx 20 -pady 0
        bind $base.cl <1> "::InstallAPI::ExecuteAction -action ShowChangelog"
    }

    if {[string trim $urltext] ne ""} {
        Label $base.url -foreground "blue" -text $urltext -autowrap 1 -anchor nw -justify left -bg #FFFFFF
        grid $base.url -row 4 -column 1 -sticky nwe -padx 20 -pady 0
        bind $base.url <1> "::InstallAPI::ExecuteAction -action OpenWebSite"
    }

    Label $base.footerspace -bg #FFFFFF -autowrap 1 -text " "
    grid  $base.footerspace -row 5 -column 1 -sticky news -padx 20

    grid $base.sep -row 6 -column 0 -columnspan 2 -sticky ew
}

