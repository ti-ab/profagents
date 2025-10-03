import "@/styles/globals.css";
import type {AppProps} from "next/app";
import {useRouter} from "next/router";
import * as React from "react";
import Box from "@mui/material/Box";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemIcon from "@mui/material/ListItemIcon";
import SchoolIcon from "@mui/icons-material/School";
import ListItemText from "@mui/material/ListItemText";
import Divider from "@mui/material/Divider";
import {Button, Drawer, Fab} from "@mui/material";
import HomeIcon from '@mui/icons-material/Home';
import ReorderIcon from '@mui/icons-material/Reorder';

export default function App({Component, pageProps}: AppProps) {

    const router = useRouter();

    const [open, setOpen] = React.useState(false);

    const toggleDrawer = (newOpen: boolean) => () => {
        setOpen(newOpen);
    };

    const DrawerList = (
        <Box sx={{width: 250}} role="presentation" onClick={toggleDrawer(false)}>
            <List>
                <ListItem disablePadding>
                    <ListItemButton onClick={() => router.push("/")}>
                        <ListItemIcon>
                            <HomeIcon/>
                        </ListItemIcon>
                        <ListItemText primary={"Home"}/>
                    </ListItemButton>
                </ListItem>
                <Divider/>
                <ListItem disablePadding>
                    <ListItemButton onClick={() => router.push("/courses")}>
                        <ListItemIcon>
                            <SchoolIcon/>
                        </ListItemIcon>
                        <ListItemText primary={"Courses"}/>
                    </ListItemButton>
                </ListItem>
            </List>
        </Box>
    );

    return <div>

        <Drawer open={open} onClose={toggleDrawer(false)}>
            {DrawerList}
        </Drawer>

        <div className={"p-2"}>
            <Fab
                color="primary"
                aria-label="menu"
                onClick={toggleDrawer(true)}
                style={{
                    position: 'fixed',
                    top: 16,            // distance depuis le haut
                    left: '50%',        // point de départ au centre horizontal
                    transform: 'translateX(-50%)', // recentre exactement
                    zIndex: 1300       // au-dessus des autres éléments
                }}
            >
                <ReorderIcon/>
            </Fab>
        </div>


        <Component {...pageProps} />

    </div>;
}
