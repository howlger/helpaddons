proc CreateWindow.4B36D341-ECAD-400B-8792-D806DA815850 {wizard id} {
    variable info

    CreateWindow.CustomBlankPane2 $wizard $id

    set base [$id widget get ClientArea]

    set varName [$id get VirtualText]

    grid rowconfigure    $base 0 -weight 1
    grid columnconfigure $base 0 -weight 1

    labelframe $base.frame -relief groove -bd 2
    grid $base.frame -row 0 -column 0 -sticky ew
    $id widget set DestinationLabel -widget $base.frame

    grid rowconfigure    $base.frame 0 -weight 1
    grid columnconfigure $base.frame 0 -weight 1

    Label $base.frame.destination -anchor nw -elide 1 -elideside center  -ellipsis {[...]}
    grid  $base.frame.destination -row 0 -column 0 -sticky ew -padx 5 -pady 3
    $id widget set Destination -widget $base.frame.destination
    if {$varName ne ""} {
        $id setText all Destination "<%Dir <%$varName%>%>"
    }

    Button $base.frame.browse -command  [list ::InstallAPI::PromptForDirectory -virtualtext $varName]
    grid $base.frame.browse -row 0 -column 1 -sticky nw -padx 5 -pady [list 0 5]
    $id widget set BrowseButton -widget $base.frame.browse
}

